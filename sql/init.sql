-- ============================================================================
-- EAMS企业资产管理系统 V1.0 — 数据库初始化脚本
-- 版本: V1.0  日期: 2026-06-24
-- 环境要求: MySQL 8.0+  InnoDB  utf8mb4
-- 使用方式: mysql -u root -p < sql/init.sql
-- ============================================================================

-- 创建数据库
DROP DATABASE IF EXISTS `eams`;
CREATE DATABASE `eams` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `eams`;

-- ============================================================================
-- 第一部分: 系统权限表 (10张)
-- ============================================================================

-- 1. 用户表
CREATE TABLE `sys_user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录账号，全局唯一',
    `password` VARCHAR(200) NOT NULL COMMENT '登录密码（BCrypt加密存储）',
    `real_name` VARCHAR(50) NOT NULL COMMENT '员工真实姓名',
    `dept_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属部门ID (FK -> sys_dept.id)',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号（11位，全局唯一校验）',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱地址（全局唯一校验）',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '账号状态: 0-禁用, 1-启用',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最近一次登录物理外键是指什么？成功时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最近一次登录来源IP地址',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_dept_id` (`dept_id`),
    KEY `idx_sys_user_phone` (`phone`),
    KEY `idx_sys_user_status` (`status`),
    KEY `idx_sys_user_real_name` (`real_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统用户表';

-- 2. 角色表
CREATE TABLE `sys_role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称（中文展示）',
    `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码: ROLE_SUPER_ADMIN/ROLE_ASSET_ADMIN/ROLE_DEPT_ADMIN/ROLE_EMPLOYEE',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '角色职责描述',
    `is_system` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否系统预置角色: 0-自定义, 1-系统预置(不可删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统角色表';

-- 3. 用户-角色关联表
CREATE TABLE `sys_user_role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID (FK -> sys_user.id)',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID (FK -> sys_role.id)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
    KEY `idx_sys_user_role_user_id` (`user_id`),
    KEY `idx_sys_user_role_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 4. 部门表
CREATE TABLE `sys_dept` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级部门ID: 0表示一级部门 (FK -> sys_dept.id)',
    `dept_name` VARCHAR(50) NOT NULL COMMENT '部门名称（同级不可重名）',
    `dept_code` VARCHAR(50) NOT NULL COMMENT '部门编码（全局唯一）: DEPT_XXXX',
    `leader_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '部门负责人ID (FK -> sys_user.id)',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '同级排序号: 0-9999，值越小越靠前',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '部门状态: 0-禁用, 1-启用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dept_dept_code` (`dept_code`),
    KEY `idx_sys_dept_parent_id` (`parent_id`),
    KEY `idx_sys_dept_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统部门表';

-- 5. 菜单权限表
CREATE TABLE `sys_menu` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级菜单ID: 0表示顶级 (FK -> sys_menu.id)',
    `menu_name` VARCHAR(50) NOT NULL COMMENT '菜单名称（前端展示）',
    `menu_type` CHAR(1) NOT NULL DEFAULT 'M' COMMENT '菜单类型: M-目录, P-页面, B-按钮',
    `path` VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径 / 权限标识',
    `component` VARCHAR(200) DEFAULT NULL COMMENT '前端Vue组件路径（仅页面级）',
    `permission` VARCHAR(100) DEFAULT NULL COMMENT '后端权限标识: system:user:add',
    `icon` VARCHAR(50) DEFAULT NULL COMMENT 'Element Plus图标名称',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '同级排序号',
    `visible` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否可见: 0-隐藏, 1-显示',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_menu_parent_id` (`parent_id`),
    KEY `idx_sys_menu_menu_type` (`menu_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统菜单权限表';

-- 6. 角色-菜单关联表
CREATE TABLE `sys_role_menu` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID (FK -> sys_role.id)',
    `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单ID (FK -> sys_menu.id)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_menu` (`role_id`, `menu_id`),
    KEY `idx_sys_role_menu_role_id` (`role_id`),
    KEY `idx_sys_role_menu_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 7. 字典类型表
CREATE TABLE `sys_dict_type` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_name` VARCHAR(50) NOT NULL COMMENT '字典名称',
    `dict_code` VARCHAR(50) NOT NULL COMMENT '字典编码（全局唯一）',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `description` VARCHAR(200) DEFAULT NULL COMMENT '字典用途描述',
    `is_system` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否系统预置: 0-自定义, 1-系统预置(不可删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dict_type_dict_code` (`dict_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统字典类型表';

-- 8. 字典项表
CREATE TABLE `sys_dict_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dict_code` VARCHAR(50) NOT NULL COMMENT '所属字典编码 (FK -> sys_dict_type.dict_code)',
    `dict_label` VARCHAR(100) NOT NULL COMMENT '字典显示文本',
    `dict_value` VARCHAR(50) NOT NULL COMMENT '字典编码值',
    `css_class` VARCHAR(50) DEFAULT NULL COMMENT '前端标签样式: primary/success/warning/danger',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号: 0-9999',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `is_system` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否系统预置核心项: 0-普通, 1-核心(不可删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_dict_item` (`dict_code`, `dict_value`),
    KEY `idx_sys_dict_item_dict_code` (`dict_code`),
    KEY `idx_sys_dict_item_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统字典项表';

-- 9. 系统参数配置表
CREATE TABLE `sys_config` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `param_key` VARCHAR(100) NOT NULL COMMENT '参数键名',
    `param_name` VARCHAR(50) NOT NULL COMMENT '参数中文名称',
    `param_value` VARCHAR(500) NOT NULL COMMENT '当前生效的参数值',
    `param_type` VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT '参数值类型: TEXT/NUMBER/SWITCH/JSON',
    `param_group` VARCHAR(50) NOT NULL COMMENT '参数分组: 用户管理/资产管理/领用管理/AI查询/系统安全',
    `default_value` VARCHAR(500) NOT NULL COMMENT '默认值（禁用时回退）',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用(使用默认值), 1-启用',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '参数用途及修改影响说明',
    `is_system` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否系统预置: 0-自定义, 1-系统预置(不可删除)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_config_param_key` (`param_key`),
    KEY `idx_sys_config_param_group` (`param_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统参数配置表';

-- 10. 参数变更历史表
CREATE TABLE `sys_config_history` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_id` BIGINT UNSIGNED NOT NULL COMMENT '关联参数ID (FK -> sys_config.id)',
    `param_key` VARCHAR(100) NOT NULL COMMENT '参数键名快照',
    `old_value` VARCHAR(500) DEFAULT NULL COMMENT '修改前的旧值',
    `new_value` VARCHAR(500) NOT NULL COMMENT '修改后的新值',
    `change_reason` VARCHAR(200) DEFAULT NULL COMMENT '修改原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_config_history_config_id` (`config_id`),
    KEY `idx_sys_config_history_param_key` (`param_key`),
    KEY `idx_sys_config_history_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统参数变更历史表';

-- ============================================================================
-- 第二部分: 系统日志表 (2张)
-- ============================================================================

-- 11. 操作日志表
CREATE TABLE `sys_operation_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `operator` VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    `module` VARCHAR(50) NOT NULL COMMENT '操作模块',
    `action_type` VARCHAR(20) NOT NULL COMMENT '操作类型: 新增/编辑/删除/导入/导出/登录/审批/查询',
    `description` VARCHAR(500) NOT NULL COMMENT '操作描述摘要',
    `request_params` TEXT DEFAULT NULL COMMENT 'HTTP请求参数(JSON)',
    `before_data` TEXT DEFAULT NULL COMMENT '变更前数据快照(JSON)',
    `after_data` TEXT DEFAULT NULL COMMENT '变更后数据快照(JSON)',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作来源IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器User-Agent',
    `cost_time` INT UNSIGNED DEFAULT NULL COMMENT '接口响应耗时（毫秒）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作发生时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 恒为0（审计日志不可删除）',
    PRIMARY KEY (`id`),
    KEY `idx_sys_operation_log_operator` (`operator`),
    KEY `idx_sys_operation_log_module` (`module`),
    KEY `idx_sys_operation_log_action_type` (`action_type`),
    KEY `idx_sys_operation_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统操作日志表（审计不可删除）';

-- 12. 登录日志表
CREATE TABLE `sys_login_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '尝试登录的用户名',
    `login_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '登录结果: 0-失败, 1-成功',
    `fail_reason` VARCHAR(100) DEFAULT NULL COMMENT '失败原因',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '登录来源IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器User-Agent',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录尝试时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_sys_login_log_username` (`username`),
    KEY `idx_sys_login_log_login_status` (`login_status`),
    KEY `idx_sys_login_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统登录日志表';

-- ============================================================================
-- 第三部分: 资产台账表 (3张)
-- ============================================================================

-- 13. 资产信息表
CREATE TABLE `asset_info` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `asset_code` VARCHAR(30) NOT NULL COMMENT '资产编码（全局唯一）: AS-{类别码}-{YYMM}-{4位流水}',
    `asset_name` VARCHAR(100) NOT NULL COMMENT '资产名称',
    `category` VARCHAR(50) NOT NULL COMMENT '资产分类（字典值） (FK -> sys_dict_item.dict_value)',
    `specification` VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    `sn_number` VARCHAR(100) DEFAULT NULL COMMENT 'SN序列号',
    `procurement_no` VARCHAR(50) DEFAULT NULL COMMENT '关联采购单号 (FK -> proc_order.procurement_no)',
    `procurement_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联采购记录ID (FK -> proc_order.id)',
    `original_value` DECIMAL(12,2) NOT NULL COMMENT '资产采购原值（元）',
    `purchase_date` DATE NOT NULL COMMENT '采购日期',
    `useful_life` TINYINT UNSIGNED NOT NULL COMMENT '预计使用年限（年）: 1-50',
    `residual_rate` DECIMAL(5,2) NOT NULL DEFAULT 5.00 COMMENT '净残值率(%): 默认5%',
    `scrap_date` DATE DEFAULT NULL COMMENT '预计报废日期（后端自动计算）',
    `location` VARCHAR(100) NOT NULL COMMENT '存放地点',
    `dept_id` BIGINT UNSIGNED NOT NULL COMMENT '资产归属部门ID (FK -> sys_dept.id)',
    `user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '当前使用人ID: 闲置为NULL (FK -> sys_user.id)',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '资产状态: 0-闲置, 1-在用, 2-借用, 3-维修, 4-报废, 5-盘点中',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '资产照片URL',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '资产录入时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '录入人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_info_asset_code` (`asset_code`),
    KEY `idx_asset_info_category` (`category`),
    KEY `idx_asset_info_status` (`status`),
    KEY `idx_asset_info_dept_id` (`dept_id`),
    KEY `idx_asset_info_user_id` (`user_id`),
    KEY `idx_asset_info_purchase_date` (`purchase_date`),
    KEY `idx_asset_info_location` (`location`),
    KEY `idx_asset_info_asset_name` (`asset_name`),
    KEY `idx_asset_info_status_dept` (`status`, `dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产信息表（核心主表）';

-- 14. 资产附件表
CREATE TABLE `asset_attachment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '所属资产ID (FK -> asset_info.id)',
    `file_name` VARCHAR(200) NOT NULL COMMENT '原始上传文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '服务器存储路径',
    `file_size` BIGINT UNSIGNED NOT NULL COMMENT '文件大小（字节）',
    `file_type` VARCHAR(20) NOT NULL COMMENT '文件类型: IMAGE-资产图片, FILE-附件',
    `mime_type` VARCHAR(50) DEFAULT NULL COMMENT 'MIME类型',
    `sort_order` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '附件排序号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '上传人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_asset_attachment_asset_id` (`asset_id`),
    KEY `idx_asset_attachment_file_type` (`file_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产附件表';

-- 15. 折旧明细表
CREATE TABLE `asset_depreciation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '资产ID (FK -> asset_info.id)',
    `depreciation_month` VARCHAR(7) NOT NULL COMMENT '计提月份: 2026-06',
    `monthly_amount` DECIMAL(12,2) NOT NULL COMMENT '本月折旧额（元）',
    `accumulated` DECIMAL(12,2) NOT NULL COMMENT '截至本月的累计折旧额（元）',
    `net_value` DECIMAL(12,2) NOT NULL COMMENT '截至本月的资产净值（元）= 原值 - 累计折旧',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '计提状态: 0-待计提, 1-已计提',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_asset_depreciation` (`asset_id`, `depreciation_month`),
    KEY `idx_asset_depreciation_asset_id` (`asset_id`),
    KEY `idx_asset_depreciation_month` (`depreciation_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产折旧明细表';

-- ============================================================================
-- 第四部分: 采购入库表 (2张)
-- ============================================================================

-- 16. 采购入库记录表
CREATE TABLE `proc_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `procurement_no` VARCHAR(50) DEFAULT NULL COMMENT '采购单号（外部采购单据号）',
    `asset_name` VARCHAR(100) NOT NULL COMMENT '采购资产名称',
    `category` VARCHAR(50) NOT NULL COMMENT '资产分类（字典值） (FK -> sys_dict_item.dict_value)',
    `specification` VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
    `sn_number` VARCHAR(100) DEFAULT NULL COMMENT 'SN序列号',
    `quantity` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '采购数量: 1-100',
    `unit_price` DECIMAL(12,2) NOT NULL COMMENT '单价（元）',
    `total_amount` DECIMAL(12,2) NOT NULL COMMENT '采购总价（元）= 单价 × 数量',
    `purchase_date` DATE NOT NULL COMMENT '采购日期',
    `supplier_id` BIGINT UNSIGNED NOT NULL COMMENT '供应商ID (FK -> proc_supplier.id)',
    `useful_life` TINYINT UNSIGNED NOT NULL COMMENT '预计使用年限（年）: 1-50',
    `residual_rate` DECIMAL(5,2) NOT NULL DEFAULT 5.00 COMMENT '净残值率(%): 默认5%',
    `dept_id` BIGINT UNSIGNED NOT NULL COMMENT '资产归属部门ID (FK -> sys_dept.id)',
    `location` VARCHAR(100) NOT NULL COMMENT '存放地点',
    `accept_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '验收状态: 0-待验收, 1-已验收, 2-已入库, 3-已取消',
    `accept_date` DATE DEFAULT NULL COMMENT '验收日期',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '采购登记时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '登记人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_proc_order_supplier_id` (`supplier_id`),
    KEY `idx_proc_order_accept_status` (`accept_status`),
    KEY `idx_proc_order_purchase_date` (`purchase_date`),
    KEY `idx_proc_order_category` (`category`),
    KEY `idx_proc_order_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='采购入库记录表';

-- 17. 供应商信息表
CREATE TABLE `proc_supplier` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `supplier_name` VARCHAR(100) NOT NULL COMMENT '供应商名称（全局唯一）',
    `supplier_code` VARCHAR(50) NOT NULL COMMENT '供应商编码（全局唯一）: SUP-XXXX',
    `contact_person` VARCHAR(20) DEFAULT NULL COMMENT '联系人姓名',
    `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系人手机号',
    `address` VARCHAR(200) DEFAULT NULL COMMENT '供应商地址',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_proc_supplier_name` (`supplier_name`),
    UNIQUE KEY `uk_proc_supplier_code` (`supplier_code`),
    KEY `idx_proc_supplier_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='供应商信息表';

-- ============================================================================
-- 第五部分: 领用归还表 (2张)
-- ============================================================================

-- 18. 领用申请单表
CREATE TABLE `req_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `apply_no` VARCHAR(30) NOT NULL COMMENT '申请编号（全局唯一）: RY-YYYYMMDD-XXXX',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '领用资产ID (FK -> asset_info.id)',
    `applicant_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人ID (FK -> sys_user.id)',
    `applicant_dept_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人所属部门ID (FK -> sys_dept.id)',
    `purpose` VARCHAR(500) NOT NULL COMMENT '领用用途说明: 10-500字符',
    `expect_duration` VARCHAR(20) NOT NULL COMMENT '预计领用时长: 1个月/3个月/6个月/12个月/自定义',
    `expect_return_date` DATE NOT NULL COMMENT '预计归还日期',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '领用状态: 0-待部门审批, 1-待资产管理员审批, 2-已通过(在用), 3-已驳回, 4-已归还',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '申请人备注',
    `return_date` DATE DEFAULT NULL COMMENT '实际归还日期',
    `return_asset_status` TINYINT UNSIGNED DEFAULT NULL COMMENT '归还时资产完好: 0-完好, 1-有损坏',
    `return_damage_desc` VARCHAR(500) DEFAULT NULL COMMENT '损坏情况说明',
    `return_remark` VARCHAR(200) DEFAULT NULL COMMENT '归还备注',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '申请人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_req_order_apply_no` (`apply_no`),
    KEY `idx_req_order_asset_id` (`asset_id`),
    KEY `idx_req_order_applicant_id` (`applicant_id`),
    KEY `idx_req_order_status` (`status`),
    KEY `idx_req_order_expect_return_date` (`expect_return_date`),
    KEY `idx_req_order_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='领用申请单表';

-- 19. 领用审批日志表
CREATE TABLE `req_approval_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `requisition_id` BIGINT UNSIGNED NOT NULL COMMENT '领用申请单ID (FK -> req_order.id)',
    `approver_id` BIGINT UNSIGNED NOT NULL COMMENT '审批人ID (FK -> sys_user.id)',
    `approval_level` TINYINT UNSIGNED NOT NULL COMMENT '审批级别: 1-部门管理员, 2-资产管理员',
    `approval_result` TINYINT UNSIGNED NOT NULL COMMENT '审批结果: 0-驳回, 1-通过',
    `reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '驳回原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批操作时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '审批人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_req_approval_log_req_id` (`requisition_id`),
    KEY `idx_req_approval_log_approver_id` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='领用审批日志表';

-- ============================================================================
-- 第六部分: 资产调拨/维保/报废/盘点/AI (7张)
-- ============================================================================

-- 20. 调拨申请单表
CREATE TABLE `trans_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `transfer_no` VARCHAR(30) NOT NULL COMMENT '调拨编号（全局唯一）: DB-YYYYMMDD-XXXX',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '调拨资产ID (FK -> asset_info.id)',
    `from_dept_id` BIGINT UNSIGNED NOT NULL COMMENT '调出部门ID (FK -> sys_dept.id)',
    `to_dept_id` BIGINT UNSIGNED NOT NULL COMMENT '调入部门ID (FK -> sys_dept.id)',
    `to_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '调入后使用人ID (FK -> sys_user.id)',
    `to_location` VARCHAR(100) NOT NULL COMMENT '调入后存放地点',
    `transfer_reason` VARCHAR(500) NOT NULL COMMENT '调拨原因: 10-500字符',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '调拨状态: 0-待调入确认, 1-待资产管理员审批, 2-已通过, 3-已驳回',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
    `applicant_id` BIGINT UNSIGNED NOT NULL COMMENT '调拨申请人ID (FK -> sys_user.id)',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调拨申请时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '申请人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trans_order_transfer_no` (`transfer_no`),
    KEY `idx_trans_order_asset_id` (`asset_id`),
    KEY `idx_trans_order_from_dept_id` (`from_dept_id`),
    KEY `idx_trans_order_to_dept_id` (`to_dept_id`),
    KEY `idx_trans_order_status` (`status`),
    KEY `idx_trans_order_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产调拨申请单表';

-- 21. 报修单表
CREATE TABLE `repair_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `repair_no` VARCHAR(30) NOT NULL COMMENT '报修编号（全局唯一）: WX-YYYYMMDD-XXXX',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '报修资产ID (FK -> asset_info.id)',
    `applicant_id` BIGINT UNSIGNED NOT NULL COMMENT '报修人ID (FK -> sys_user.id)',
    `fault_type` VARCHAR(20) NOT NULL COMMENT '故障类型: 硬件故障/软件故障/网络故障/配件更换/其他',
    `urgency` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '紧急程度: 0-普通, 1-紧急',
    `fault_desc` VARCHAR(500) NOT NULL COMMENT '故障详细描述: 10-500字符',
    `fault_images` VARCHAR(1000) DEFAULT NULL COMMENT '故障现场图片URL（逗号分隔）',
    `contact_phone` VARCHAR(20) NOT NULL COMMENT '报修人联系电话',
    `repair_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '维修状态: 0-待维修, 1-维修中, 2-已修复, 3-无法修复',
    `pre_repair_status` TINYINT UNSIGNED DEFAULT NULL COMMENT '维修前资产状态快照',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '报修备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报修提交时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '报修人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_repair_order_repair_no` (`repair_no`),
    KEY `idx_repair_order_asset_id` (`asset_id`),
    KEY `idx_repair_order_applicant_id` (`applicant_id`),
    KEY `idx_repair_order_repair_status` (`repair_status`),
    KEY `idx_repair_order_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产报修单表';

-- 22. 维修处理记录表
CREATE TABLE `repair_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `repair_order_id` BIGINT UNSIGNED NOT NULL COMMENT '报修单ID (FK -> repair_order.id)',
    `repair_method` VARCHAR(20) NOT NULL COMMENT '维修方式: 现场维修/送修/上门维修/远程支持',
    `repair_person` VARCHAR(50) NOT NULL COMMENT '维修人员姓名',
    `repair_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '维修费用（元）',
    `start_date` DATE NOT NULL COMMENT '开始维修日期',
    `finish_date` DATE DEFAULT NULL COMMENT '实际修复完成日期',
    `fault_reason` VARCHAR(500) NOT NULL COMMENT '故障根因分析',
    `solution` VARCHAR(500) NOT NULL COMMENT '处理措施描述',
    `repair_files` VARCHAR(1000) DEFAULT NULL COMMENT '维修相关附件URL（逗号分隔）',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '维修备注',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '维修记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '维修处理人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_repair_record_repair_order_id` (`repair_order_id`),
    KEY `idx_repair_record_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='维修处理记录表';

-- 23. 报废申请单表
CREATE TABLE `scrap_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `scrap_no` VARCHAR(30) NOT NULL COMMENT '报废编号（全局唯一）: BF-YYYYMMDD-XXXX',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '报废资产ID (FK -> asset_info.id)',
    `scrap_reason` VARCHAR(20) NOT NULL COMMENT '报废原因: 老化损坏/技术淘汰/维修成本过高/盘亏确认/其他',
    `reason_desc` VARCHAR(500) NOT NULL COMMENT '报废原因详细说明',
    `disposal_advice` VARCHAR(20) NOT NULL COMMENT '处置方式建议: 变卖/回收/销毁/其他',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '报废状态: 0-待初审, 1-待终审, 2-已通过, 3-已驳回, 4-已处置',
    `applicant_id` BIGINT UNSIGNED NOT NULL COMMENT '报废申请人ID (FK -> sys_user.id)',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '申请备注',
    `disposal_method` VARCHAR(20) DEFAULT NULL COMMENT '实际处置方式: 变卖/回收/销毁/其他',
    `disposal_date` DATE DEFAULT NULL COMMENT '实际处置日期',
    `disposal_income` DECIMAL(12,2) DEFAULT NULL COMMENT '处置收入（元）',
    `disposal_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '处置费用（元）',
    `disposal_handler` VARCHAR(50) DEFAULT NULL COMMENT '处置经办人姓名',
    `disposal_desc` VARCHAR(500) DEFAULT NULL COMMENT '处置过程说明',
    `attachment_urls` VARCHAR(1000) DEFAULT NULL COMMENT '报废/处置相关附件URL（逗号分隔）',
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报废申请时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '申请人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_scrap_order_scrap_no` (`scrap_no`),
    KEY `idx_scrap_order_asset_id` (`asset_id`),
    KEY `idx_scrap_order_status` (`status`),
    KEY `idx_scrap_order_applicant_id` (`applicant_id`),
    KEY `idx_scrap_order_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产报废申请单表';

-- 24. 盘点任务表
CREATE TABLE `inv_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_no` VARCHAR(30) NOT NULL COMMENT '任务编号（全局唯一）: PD-YYYYMMDD-XXX',
    `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `scope_type` VARCHAR(20) NOT NULL COMMENT '范围类型: ALL/DEPT/CATEGORY',
    `scope_value` VARCHAR(500) DEFAULT NULL COMMENT '范围值JSON',
    `inventory_date` DATE NOT NULL COMMENT '计划盘点日期',
    `total_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '应盘资产总数',
    `checked_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已确认资产数',
    `normal_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '盘点正常数量',
    `surplus_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '盘盈数量',
    `shortage_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '盘亏数量',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '盘点状态: 0-进行中, 1-已完成, 2-已取消',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '任务备注',
    `creator_id` BIGINT UNSIGNED NOT NULL COMMENT '任务创建人ID (FK -> sys_user.id)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inv_task_task_no` (`task_no`),
    KEY `idx_inv_task_status` (`status`),
    KEY `idx_inv_task_inventory_date` (`inventory_date`),
    KEY `idx_inv_task_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='盘点任务表';

-- 25. 盘点明细表
CREATE TABLE `inv_detail` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT UNSIGNED NOT NULL COMMENT '所属盘点任务ID (FK -> inv_task.id)',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '资产ID (FK -> asset_info.id)',
    `book_user_name` VARCHAR(50) DEFAULT NULL COMMENT '账面使用人快照',
    `book_status` TINYINT UNSIGNED DEFAULT NULL COMMENT '账面资产状态快照',
    `inventory_result` TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '盘点结果: 0-盘盈, 1-盘亏, 2-正常',
    `remark` VARCHAR(200) DEFAULT NULL COMMENT '实盘现场备注',
    `is_confirmed` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已确认: 0-未确认, 1-已确认',
    `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '明细创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_inv_detail_task_id` (`task_id`),
    KEY `idx_inv_detail_asset_id` (`asset_id`),
    KEY `idx_inv_detail_result` (`inventory_result`),
    KEY `idx_inv_detail_is_confirmed` (`is_confirmed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='盘点明细表';

-- 26. 盘点差异表
CREATE TABLE `inv_difference` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `task_id` BIGINT UNSIGNED NOT NULL COMMENT '所属盘点任务ID (FK -> inv_task.id)',
    `detail_id` BIGINT UNSIGNED NOT NULL COMMENT '盘点明细ID (FK -> inv_detail.id)',
    `diff_type` TINYINT UNSIGNED NOT NULL COMMENT '差异类型: 0-盘盈, 1-盘亏',
    `asset_id` BIGINT UNSIGNED NOT NULL COMMENT '关联资产ID (FK -> asset_info.id)',
    `asset_name` VARCHAR(100) NOT NULL COMMENT '资产名称快照',
    `asset_code` VARCHAR(30) NOT NULL COMMENT '资产编码快照',
    `book_qty` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '账面数量',
    `actual_qty` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '实盘数量',
    `diff_desc` VARCHAR(200) DEFAULT NULL COMMENT '差异原因说明',
    `handle_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '处理状态: 0-待处理, 1-已处理',
    `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID (FK -> sys_user.id)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '差异记录创建时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 0-正常, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_inv_difference_task_id` (`task_id`),
    KEY `idx_inv_difference_diff_type` (`diff_type`),
    KEY `idx_inv_difference_handle_status` (`handle_status`),
    KEY `idx_inv_difference_asset_id` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='盘点差异表';

-- 27. AI查询日志表
CREATE TABLE `ai_query_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '查询用户ID (FK -> sys_user.id)',
    `user_name` VARCHAR(50) NOT NULL COMMENT '查询用户名快照',
    `query_text` VARCHAR(500) NOT NULL COMMENT '用户原始自然语言输入',
    `parsed_result` TEXT DEFAULT NULL COMMENT 'DeepSeek解析出的查询条件(JSON)',
    `result_count` INT UNSIGNED DEFAULT NULL COMMENT '返回的资产记录条数',
    `cost_time` INT UNSIGNED DEFAULT NULL COMMENT '接口响应耗时（毫秒）',
    `status` VARCHAR(20) NOT NULL COMMENT '查询状态: SUCCESS/FAIL/TIMEOUT/RATE_LIMITED',
    `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '错误信息',
    `request_body` TEXT DEFAULT NULL COMMENT '完整请求体(JSON)',
    `response_body` TEXT DEFAULT NULL COMMENT '完整响应体(JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '查询执行时间',
    `create_by` VARCHAR(50) DEFAULT NULL COMMENT '查询人用户名',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后修改时间',
    `update_by` VARCHAR(50) DEFAULT NULL COMMENT '最后修改人用户名',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记: 恒为0（日志不可删除）',
    PRIMARY KEY (`id`),
    KEY `idx_ai_query_log_user_id` (`user_id`),
    KEY `idx_ai_query_log_status` (`status`),
    KEY `idx_ai_query_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI查询日志表（不可删除）';

-- ============================================================================
-- 第七部分: 初始化基础数据
-- ============================================================================

-- 7.1 部门数据（7条）
INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `dept_code`, `leader_id`, `sort_order`, `status`, `create_by`) VALUES
(1, 0, 'XX科技有限公司', 'DEPT_ROOT', NULL, 0, 1, 'SYSTEM'),
(2, 1, '技术部',     'DEPT_TECH',     NULL, 1, 1, 'SYSTEM'),
(3, 2, '研发一组',    'DEPT_TECH_RD1', NULL, 1, 1, 'SYSTEM'),
(4, 2, '研发二组',    'DEPT_TECH_RD2', NULL, 2, 1, 'SYSTEM'),
(5, 1, '产品部',     'DEPT_PRODUCT',  NULL, 3, 1, 'SYSTEM'),
(6, 1, '行政部',     'DEPT_ADMIN',    NULL, 4, 1, 'SYSTEM'),
(7, 1, '财务部',     'DEPT_FINANCE',  NULL, 5, 1, 'SYSTEM'),
(8, 1, '市场部',     'DEPT_MARKET',   NULL, 6, 1, 'SYSTEM');

-- 7.2 用户数据（5个演示账号，密码均为 Eams@123456，BCrypt加密）
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `dept_id`, `phone`, `email`, `status`, `create_by`) VALUES
(1, 'admin',      '$2a$10$tBXv7BP01kwFK0MEk1q/W.SItxeN1Uc488kFkeX/WMRMQ3FSvGVQC', '系统管理员', 6, '13800000001', 'admin@eams.com',      1, 'SYSTEM'),
(2, 'assetadmin', '$2a$10$tBXv7BP01kwFK0MEk1q/W.SItxeN1Uc488kFkeX/WMRMQ3FSvGVQC', '资产管理员', 6, '13800000002', 'assetadmin@eams.com', 1, 'SYSTEM'),
(3, 'deptadmin',  '$2a$10$tBXv7BP01kwFK0MEk1q/W.SItxeN1Uc488kFkeX/WMRMQ3FSvGVQC', '技术部主管', 2, '13800000003', 'deptadmin@eams.com',  1, 'SYSTEM'),
(4, 'zhangsan',   '$2a$10$tBXv7BP01kwFK0MEk1q/W.SItxeN1Uc488kFkeX/WMRMQ3FSvGVQC', '张三',       2, '13800000004', 'zhangsan@eams.com',   1, 'SYSTEM'),
(5, 'lisi',       '$2a$10$tBXv7BP01kwFK0MEk1q/W.SItxeN1Uc488kFkeX/WMRMQ3FSvGVQC', '李四',       5, '13800000005', 'lisi@eams.com',       1, 'SYSTEM');

-- 7.3 角色数据（4个预置角色）
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `is_system`, `create_by`) VALUES
(1, '超级管理员',  'ROLE_SUPER_ADMIN',  '系统最高权限，拥有全部功能访问权限及系统配置权限', 1, 'SYSTEM'),
(2, '资产管理员',  'ROLE_ASSET_ADMIN',  '负责资产全生命周期管理，拥有业务权限，无系统配置权限', 1, 'SYSTEM'),
(3, '部门管理员',  'ROLE_DEPT_ADMIN',   '管理本部门资产，可查看本部门数据，审批本部门员工领用申请', 1, 'SYSTEM'),
(4, '普通员工',    'ROLE_EMPLOYEE',     '可申请领用资产、归还本人已领用资产、报修、AI查询', 1, 'SYSTEM');

-- 7.4 用户-角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1), -- admin → 超级管理员
(2, 2), -- assetadmin → 资产管理员
(3, 3), -- deptadmin → 部门管理员
(4, 4), -- zhangsan → 普通员工
(5, 4); -- lisi → 普通员工

-- 7.5 菜单权限树（9个模块: M-目录, P-页面, B-按钮）
-- ======== 系统管理 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(1,  0,  '系统管理',   'M', '/system',        NULL,          NULL,                 'Setting',      1, 'SYSTEM'),
(2,  1,  '用户管理',   'P', '/system/user',    'system/user/index',   'system:user:list',    'User',        1, 'SYSTEM'),
(3,  2,  '新增用户',   'B', NULL,              NULL,                  'system:user:add',     NULL,          1, 'SYSTEM'),
(4,  2,  '编辑用户',   'B', NULL,              NULL,                  'system:user:edit',    NULL,          2, 'SYSTEM'),
(5,  2,  '删除用户',   'B', NULL,              NULL,                  'system:user:delete',  NULL,          3, 'SYSTEM'),
(6,  2,  '重置密码',   'B', NULL,              NULL,                  'system:user:resetPwd',NULL,          4, 'SYSTEM'),
(7,  1,  '角色管理',   'P', '/system/role',    'system/role/index',   'system:role:list',    'Avatar',      2, 'SYSTEM'),
(8,  7,  '新增角色',   'B', NULL,              NULL,                  'system:role:add',     NULL,          1, 'SYSTEM'),
(9,  7,  '编辑角色',   'B', NULL,              NULL,                  'system:role:edit',    NULL,          2, 'SYSTEM'),
(10, 7,  '删除角色',   'B', NULL,              NULL,                  'system:role:delete',  NULL,          3, 'SYSTEM'),
(11, 7,  '权限配置',   'B', NULL,              NULL,                  'system:role:perm',    NULL,          4, 'SYSTEM'),
(12, 1,  '部门管理',   'P', '/system/dept',    'system/dept/index',   'system:dept:list',    'OfficeBuilding',3, 'SYSTEM'),
(13, 12, '新增部门',   'B', NULL,              NULL,                  'system:dept:add',     NULL,          1, 'SYSTEM'),
(14, 12, '编辑部门',   'B', NULL,              NULL,                  'system:dept:edit',    NULL,          2, 'SYSTEM'),
(15, 12, '删除部门',   'B', NULL,              NULL,                  'system:dept:delete',  NULL,          3, 'SYSTEM'),
(16, 1,  '数据字典',   'P', '/system/dict',    'system/dict/index',   'system:dict:list',    'Collection',  4, 'SYSTEM'),
(17, 16, '新增字典',   'B', NULL,              NULL,                  'system:dict:add',     NULL,          1, 'SYSTEM'),
(18, 16, '编辑字典',   'B', NULL,              NULL,                  'system:dict:edit',    NULL,          2, 'SYSTEM'),
(19, 16, '删除字典',   'B', NULL,              NULL,                  'system:dict:delete',  NULL,          3, 'SYSTEM'),
(20, 1,  '系统参数',   'P', '/system/config',  'system/config/index', 'system:config:list',  'SetUp',       5, 'SYSTEM'),
(21, 20, '编辑参数',   'B', NULL,              NULL,                  'system:config:edit',  NULL,          1, 'SYSTEM'),
(22, 1,  '操作日志',   'P', '/system/log',     'system/log/index',    'system:log:list',     'Document',    6, 'SYSTEM');

-- ======== 资产台账 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(30, 0,  '资产台账',   'M', '/asset',         NULL,                  NULL,                 'Box',         2, 'SYSTEM'),
(31, 30, '资产列表',   'P', '/asset/list',     'asset/list/index',    'asset:list',          'List',        1, 'SYSTEM'),
(32, 31, '查看详情',   'B', NULL,              NULL,                  'asset:detail',        NULL,          1, 'SYSTEM'),
(33, 31, '新增资产',   'B', NULL,              NULL,                  'asset:add',           NULL,          2, 'SYSTEM'),
(34, 31, '编辑资产',   'B', NULL,              NULL,                  'asset:edit',          NULL,          3, 'SYSTEM'),
(35, 31, '删除资产',   'B', NULL,              NULL,                  'asset:delete',        NULL,          4, 'SYSTEM'),
(36, 31, '导入资产',   'B', NULL,              NULL,                  'asset:import',        NULL,          5, 'SYSTEM'),
(37, 31, '导出资产',   'B', NULL,              NULL,                  'asset:export',        NULL,          6, 'SYSTEM'),
(38, 31, '折旧明细',   'B', NULL,              NULL,                  'asset:depreciation',  NULL,          7, 'SYSTEM');

-- ======== 采购入库 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(40, 0,  '采购入库',   'M', '/procurement',    NULL,                  NULL,                 'ShoppingCart', 3, 'SYSTEM'),
(41, 40, '采购登记',   'P', '/procurement/add',   'procurement/add/index',   'procurement:add',     'Plus',       1, 'SYSTEM'),
(42, 40, '供应商管理',  'P', '/procurement/supplier','procurement/supplier/index','procurement:supplier','UserFilled', 2, 'SYSTEM'),
(43, 42, '新增供应商',  'B', NULL,              NULL,                  'procurement:supplier:add',    NULL,    1, 'SYSTEM'),
(44, 42, '编辑供应商',  'B', NULL,              NULL,                  'procurement:supplier:edit',   NULL,    2, 'SYSTEM'),
(45, 42, '删除供应商',  'B', NULL,              NULL,                  'procurement:supplier:delete', NULL,    3, 'SYSTEM'),
(46, 40, '采购记录',   'P', '/procurement/record', 'procurement/record/index', 'procurement:record',  'Tickets',    3, 'SYSTEM');

-- ======== 领用管理 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(50, 0,  '领用管理',   'M', '/requisition',    NULL,                  NULL,                 'Present',      4, 'SYSTEM'),
(51, 50, '领用申请',   'P', '/requisition/apply',  'requisition/apply/index', 'requisition:apply',   'Edit',       1, 'SYSTEM'),
(52, 50, '审批管理',   'P', '/requisition/approval','requisition/approval/index','requisition:approve','Checked',   2, 'SYSTEM'),
(53, 52, '审批通过',   'B', NULL,              NULL,                  'requisition:approve:pass',    NULL,    1, 'SYSTEM'),
(54, 52, '审批驳回',   'B', NULL,              NULL,                  'requisition:approve:reject',  NULL,    2, 'SYSTEM'),
(55, 50, '归还登记',   'P', '/requisition/return',  'requisition/return/index',  'requisition:return',  'RefreshLeft',3, 'SYSTEM'),
(56, 50, '领用记录',   'P', '/requisition/record',  'requisition/record/index',  'requisition:record',  'Tickets',    4, 'SYSTEM');

-- ======== 资产调拨 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(60, 0,  '资产调拨',   'M', '/transfer',       NULL,                  NULL,                 'Switch',       5, 'SYSTEM'),
(61, 60, '调拨申请',   'P', '/transfer/apply',    'transfer/apply/index',    'transfer:apply',      'Edit',       1, 'SYSTEM'),
(62, 60, '调拨审批',   'P', '/transfer/approval', 'transfer/approval/index', 'transfer:approve',    'Checked',    2, 'SYSTEM'),
(63, 62, '确认调入',   'B', NULL,              NULL,                  'transfer:approve:confirm',    NULL,    1, 'SYSTEM'),
(64, 62, '审批通过',   'B', NULL,              NULL,                  'transfer:approve:pass',       NULL,    2, 'SYSTEM'),
(65, 62, '审批驳回',   'B', NULL,              NULL,                  'transfer:approve:reject',     NULL,    3, 'SYSTEM'),
(66, 60, '调拨记录',   'P', '/transfer/record',   'transfer/record/index',   'transfer:record',     'Tickets',    3, 'SYSTEM');

-- ======== 维保报修 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(70, 0,  '维保报修',   'M', '/repair',         NULL,                  NULL,                 'Tools',        6, 'SYSTEM'),
(71, 70, '报修登记',   'P', '/repair/apply',     'repair/apply/index',      'repair:apply',        'Edit',       1, 'SYSTEM'),
(72, 70, '维修处理',   'P', '/repair/handle',    'repair/handle/index',     'repair:handle',       'Setting',    2, 'SYSTEM'),
(73, 72, '接单',      'B', NULL,              NULL,                  'repair:handle:accept',    NULL,       1, 'SYSTEM'),
(74, 72, '填写记录',   'B', NULL,              NULL,                  'repair:handle:record',    NULL,       2, 'SYSTEM'),
(75, 72, '标记修复',   'B', NULL,              NULL,                  'repair:handle:fix',       NULL,       3, 'SYSTEM'),
(76, 70, '维保记录',   'P', '/repair/record',    'repair/record/index',     'repair:record',       'Tickets',    3, 'SYSTEM');

-- ======== 报废处置 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(80, 0,  '报废处置',   'M', '/scrap',          NULL,                  NULL,                 'Delete',       7, 'SYSTEM'),
(81, 80, '报废申请',   'P', '/scrap/apply',      'scrap/apply/index',       'scrap:apply',         'Edit',       1, 'SYSTEM'),
(82, 80, '报废审批',   'P', '/scrap/approval',   'scrap/approval/index',    'scrap:approve',       'Checked',    2, 'SYSTEM'),
(83, 82, '初审通过',   'B', NULL,              NULL,                  'scrap:approve:first',     NULL,       1, 'SYSTEM'),
(84, 82, '终审通过',   'B', NULL,              NULL,                  'scrap:approve:final',     NULL,       2, 'SYSTEM'),
(85, 82, '审批驳回',   'B', NULL,              NULL,                  'scrap:approve:reject',    NULL,       3, 'SYSTEM'),
(86, 80, '处置登记',   'P', '/scrap/disposal',   'scrap/disposal/index',    'scrap:disposal',      'Box',        3, 'SYSTEM'),
(87, 80, '报废记录',   'P', '/scrap/record',     'scrap/record/index',      'scrap:record',        'Tickets',    4, 'SYSTEM');

-- ======== 盘点管理 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(90, 0,  '盘点管理',   'M', '/inventory',      NULL,                  NULL,                 'DataAnalysis', 8, 'SYSTEM'),
(91, 90, '盘点任务',   'P', '/inventory/task',    'inventory/task/index',    'inventory:task',      'List',       1, 'SYSTEM'),
(92, 91, '创建任务',   'B', NULL,              NULL,                  'inventory:task:add',      NULL,       1, 'SYSTEM'),
(93, 91, '取消任务',   'B', NULL,              NULL,                  'inventory:task:cancel',   NULL,       2, 'SYSTEM'),
(94, 90, '执行盘点',   'P', '/inventory/execute/:taskId','inventory/execute/index','inventory:execute', 'Checked',   2, 'SYSTEM'),
(95, 94, '确认结果',   'B', NULL,              NULL,                  'inventory:execute:confirm',NULL,      1, 'SYSTEM'),
(96, 94, '完成盘点',   'B', NULL,              NULL,                  'inventory:execute:complete',NULL,     2, 'SYSTEM'),
(97, 90, '差异记录',   'P', '/inventory/difference','inventory/difference/index','inventory:difference','Warning',3, 'SYSTEM'),
(98, 97, '标记处理',   'B', NULL,              NULL,                  'inventory:difference:handle',NULL,   1, 'SYSTEM');

-- ======== AI查询 ========
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `permission`, `icon`, `sort_order`, `create_by`) VALUES
(100, 0, 'AI查询',     'M', '/ai',             NULL,                  NULL,                 'Cpu',          9, 'SYSTEM'),
(101, 100,'智能查询',   'P', '/ai/query',        'ai/query/index',          'ai:query',            'Search',     1, 'SYSTEM'),
(102, 100,'查询日志',   'P', '/ai/log',          'ai/log/index',            'ai:log',              'Document',   2, 'SYSTEM');

-- 7.6 角色-菜单权限绑定
-- 超级管理员 (role_id=1): 全部菜单
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu` WHERE `is_deleted` = 0;

-- 资产管理员 (role_id=2): 除系统管理外全部 + 操作日志
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 22), -- 操作日志
(2, 30),(2, 31),(2, 32),(2, 33),(2, 34),(2, 35),(2, 36),(2, 37),(2, 38), -- 资产台账
(2, 40),(2, 41),(2, 42),(2, 43),(2, 44),(2, 45),(2, 46), -- 采购入库
(2, 50),(2, 51),(2, 52),(2, 53),(2, 54),(2, 55),(2, 56), -- 领用管理
(2, 60),(2, 61),(2, 62),(2, 63),(2, 64),(2, 65),(2, 66), -- 资产调拨
(2, 70),(2, 71),(2, 72),(2, 73),(2, 74),(2, 75),(2, 76), -- 维保报修
(2, 80),(2, 81),(2, 82),(2, 83),(2, 84),(2, 85),(2, 86),(2, 87), -- 报废处置
(2, 90),(2, 91),(2, 92),(2, 93),(2, 94),(2, 95),(2, 96),(2, 97),(2, 98), -- 盘点管理
(2, 100),(2, 101),(2, 102); -- AI查询

-- 部门管理员 (role_id=3): 本部门资产查看/导出 + 领用申请/审批 + 报修 + AI查询
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 30),(3, 31),(3, 32),(3, 37), -- 资产台账(查看+导出)
(3, 46), -- 采购记录查看
(3, 50),(3, 51),(3, 52),(3, 53),(3, 54),(3, 56), -- 领用管理(审批本部门)
(3, 66), -- 调拨记录查看
(3, 70),(3, 71),(3, 76), -- 维保报修(报修+记录查看)
(3, 87), -- 报废记录查看
(3, 90),(3, 97), -- 盘点(差异记录查看+报告)
(3, 100),(3, 101); -- AI查询(本部门范围)

-- 普通员工 (role_id=4): 领用申请/归还 + 报修 + AI查询(本人范围)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 31),(4, 32), -- 资产台账(查看本人)
(4, 50),(4, 51),(4, 55),(4, 56), -- 领用申请+归还+本人记录
(4, 70),(4, 71),(4, 76), -- 报修登记+本人维保记录
(4, 100),(4, 101); -- AI查询(本人范围)

-- 7.7 字典数据
-- 字典类型
INSERT INTO `sys_dict_type` (`id`, `dict_name`, `dict_code`, `status`, `description`, `is_system`, `create_by`) VALUES
(1, '资产分类', 'asset_category',       1, '资产分类: IT设备、办公家具、生产设备、车辆、其他', 1, 'SYSTEM'),
(2, '资产状态', 'asset_status',         1, '资产当前业务状态', 1, 'SYSTEM'),
(3, '领用状态', 'requisition_status',   1, '领用申请单状态流转', 1, 'SYSTEM'),
(4, '折旧方法', 'depreciation_method',  1, '折旧计提方法', 1, 'SYSTEM');

-- 字典项
INSERT INTO `sys_dict_item` (`dict_code`, `dict_label`, `dict_value`, `css_class`, `sort_order`, `is_system`, `create_by`) VALUES
-- 资产分类
('asset_category', 'IT设备',   'IT_EQUIPMENT',  'primary',  1, 1, 'SYSTEM'),
('asset_category', '办公家具',  'OFFICE_FURNITURE','success', 2, 1, 'SYSTEM'),
('asset_category', '生产设备',  'PRODUCTION',     'warning',  3, 1, 'SYSTEM'),
('asset_category', '车辆',     'VEHICLE',        'danger',   4, 1, 'SYSTEM'),
('asset_category', '其他',     'OTHER',          'info',     5, 1, 'SYSTEM'),
-- 资产状态
('asset_status', '闲置',  '0', 'info',    1, 1, 'SYSTEM'),
('asset_status', '在用',  '1', 'success', 2, 1, 'SYSTEM'),
('asset_status', '借用',  '2', 'warning', 3, 1, 'SYSTEM'),
('asset_status', '维修',  '3', 'danger',  4, 1, 'SYSTEM'),
('asset_status', '报废',  '4', 'info',    5, 1, 'SYSTEM'),
('asset_status', '盘点中','5', 'warning', 6, 1, 'SYSTEM'),
-- 领用状态
('requisition_status', '待审批', '0', 'warning', 1, 1, 'SYSTEM'),
('requisition_status', '已通过', '1', 'success', 2, 1, 'SYSTEM'),
('requisition_status', '已驳回', '2', 'danger',  3, 1, 'SYSTEM'),
('requisition_status', '已归还', '3', 'info',    4, 1, 'SYSTEM'),
-- 折旧方法
('depreciation_method', '平均年限法', 'STRAIGHT_LINE', '', 1, 1, 'SYSTEM');

-- 7.8 系统参数（21个预置参数）
INSERT INTO `sys_config` (`param_key`, `param_name`, `param_value`, `param_type`, `param_group`, `default_value`, `sort_order`, `status`, `remark`, `is_system`, `create_by`) VALUES
-- 用户管理
('login.fail.max_attempts',       '登录失败锁定阈值',        '5',    'NUMBER', '用户管理', '5',    1, 1, '连续输错密码N次后触发锁定', 1, 'SYSTEM'),
('login.fail.lock_minutes',       '账号锁定时长(分钟)',      '30',   'NUMBER', '用户管理', '30',   2, 1, '锁定后自动解锁等待时间', 1, 'SYSTEM'),
('login.token.expire_hours',      'Token过期时间(小时)',     '2',    'NUMBER', '用户管理', '2',    3, 1, '普通登录Token有效期', 1, 'SYSTEM'),
('login.token.remember_hours',    '记住我Token过期(小时)',   '168',  'NUMBER', '用户管理', '168',  4, 1, '勾选记住我后有效期（168=7天）', 1, 'SYSTEM'),
('user.default_password',         '用户默认密码',            'Eams@123456', 'TEXT', '用户管理', 'Eams@123456', 5, 1, '重置密码时的默认密码', 1, 'SYSTEM'),
('user.password.min_length',      '密码最小长度',            '6',    'NUMBER', '用户管理', '6',    6, 1, '密码复杂度校验-最小长度', 1, 'SYSTEM'),
('user.password.max_length',      '密码最大长度',            '20',   'NUMBER', '用户管理', '20',   7, 1, '密码复杂度校验-最大长度', 1, 'SYSTEM'),
-- 资产管理
('asset.code.prefix',             '资产编码前缀',            'AS',   'TEXT',   '资产管理', 'AS',   1, 1, '编码格式: {前缀}-{类别码}-{YYMM}-{流水}', 1, 'SYSTEM'),
('asset.default_residual_rate',   '默认净残值率(%)',         '5',    'NUMBER', '资产管理', '5',    2, 1, '新增资产时默认填充的净残值率', 1, 'SYSTEM'),
('asset.import.max_rows',         '单次导入上限',            '1000', 'NUMBER', '资产管理', '1000', 3, 1, 'Excel批量导入单次最大条数', 1, 'SYSTEM'),
('asset.export.max_rows',         '单次导出上限',            '10000','NUMBER', '资产管理', '10000',4, 1, 'Excel导出单次最大条数', 1, 'SYSTEM'),
-- 系统安全
('upload.file.max_size_mb',       '文件上传大小限制(MB)',     '10',   'NUMBER', '系统安全', '10',   1, 1, '单文件最大体积', 1, 'SYSTEM'),
('upload.file.max_count',         '单次上传数量限制',         '5',    'NUMBER', '系统安全', '5',    2, 1, '单次最多上传文件数', 1, 'SYSTEM'),
('upload.image.max_size_mb',      '图片上传大小限制(MB)',     '2',    'NUMBER', '系统安全', '2',    3, 1, '资产图片最大体积', 1, 'SYSTEM'),
-- AI查询
('ai.rate.min_per_user',          'AI单用户分钟限流',         '10',   'NUMBER', 'AI查询',  '10',   1, 1, '每分钟单用户最多查询次数', 1, 'SYSTEM'),
('ai.rate.hour_per_user',         'AI单用户小时限流',         '100',  'NUMBER', 'AI查询',  '100',  2, 1, '每小时单用户最多查询次数', 1, 'SYSTEM'),
('ai.rate.min_per_system',        'AI全系统分钟限流',         '100',  'NUMBER', 'AI查询',  '100',  3, 1, '全系统每分钟查询上限', 1, 'SYSTEM'),
('ai.rate.daily_deepseek',        'DeepSeek日调用上限',       '5000', 'NUMBER', 'AI查询',  '5000', 4, 1, 'DeepSeek日调用超限后自动降级', 1, 'SYSTEM'),
('ai.timeout_seconds',            'AI查询超时时间(秒)',        '15',   'NUMBER', 'AI查询',  '15',   5, 1, '单次AI查询最大等待时间', 1, 'SYSTEM'),
('ai.degrade_probe_minutes',      'AI降级探测间隔(分钟)',      '5',    'NUMBER', 'AI查询',  '5',    6, 1, '降级后自动探测恢复的间隔', 1, 'SYSTEM'),
-- 领用管理
('requisition.approval_level',    '领用审批级数',             '2',    'NUMBER', '领用管理', '2',    1, 1, '一期固定2级（部门→资产管理员）', 1, 'SYSTEM');

-- 7.9 供应商样例数据
INSERT INTO `proc_supplier` (`id`, `supplier_name`, `supplier_code`, `contact_person`, `contact_phone`, `address`, `status`, `create_by`) VALUES
(1, 'XX科技有限公司',   'SUP-0001', '王经理', '13900000001', '北京市海淀区中关村科技园A座', 1, 'SYSTEM'),
(2, 'YY电子设备有限公司', 'SUP-0002', '李经理', '13900000002', '上海市浦东新区张江高科技园区', 1, 'SYSTEM');

-- ============================================================================
-- 完成
-- ============================================================================
-- 注意: 密码占位Hash非真实BCrypt值，请部署前重新生成。
-- 执行: mysql -u root -p < sql/init.sql
-- 验证: SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='eams'; -- 应返回 27
