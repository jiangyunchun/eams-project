package com.example.eams.common.config;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在 Controller 方法上，自动记录操作日志到 sys_operation_log
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** 操作模块: 系统管理/资产台账/领用管理/盘点管理/AI查询/采购入库/资产调拨/维保报修/报废处置 */
    String module();

    /** 操作类型: 新增/编辑/删除/导入/导出/登录/审批/查询 */
    String actionType();

    /** 操作描述，支持 {0} {1} 占位符引用方法参数 */
    String description() default "";
}
