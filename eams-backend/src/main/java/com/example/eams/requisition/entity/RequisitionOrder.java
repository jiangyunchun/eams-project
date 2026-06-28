package com.example.eams.requisition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 领用申请单表（PRD 6.3）
 * <p>
 * status 状态机（5 值模型，对齐二级审批）：
 * 0-待部门审批 → 1-待资产管理员审批 → 2-已通过(在用) → 4-已归还
 *                                   ↘ 3-已驳回
 */
@Data
@TableName("req_order")
public class RequisitionOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请编号（全局唯一）: RY-YYYYMMDD-XXXX */
    private String applyNo;

    /** 领用资产ID (FK -> asset_info.id) */
    private Long assetId;

    /** 申请人ID (FK -> sys_user.id) */
    private Long applicantId;

    /** 申请人所属部门ID (FK -> sys_dept.id) */
    private Long applicantDeptId;

    /** 领用用途说明: 10-500字符 */
    private String purpose;

    /** 预计领用时长: 1个月/3个月/6个月/12个月/自定义 */
    private String expectDuration;

    /** 预计归还日期 */
    private LocalDate expectReturnDate;

    /** 领用状态: 0-待部门审批, 1-待资产管理员审批, 2-已通过(在用), 3-已驳回, 4-已归还 */
    private Integer status;

    /** 申请人备注 */
    private String remark;

    /** 实际归还日期 */
    private LocalDate returnDate;

    /** 归还时资产完好: 0-完好, 1-有损坏 */
    private Integer returnAssetStatus;

    /** 损坏情况说明 */
    private String returnDamageDesc;

    /** 归还备注 */
    private String returnRemark;

    /** 乐观锁版本号 */
    private Integer version;

    // ---- transient fields (not in DB) ----

    /** 资产编码 */
    @TableField(exist = false)
    private String assetCode;

    /** 资产名称 */
    @TableField(exist = false)
    private String assetName;

    /** 资产分类 */
    @TableField(exist = false)
    private String category;

    /** 规格型号 */
    @TableField(exist = false)
    private String specification;

    /** 存放地点 */
    @TableField(exist = false)
    private String location;

    /** 资产图片URL */
    @TableField(exist = false)
    private String imageUrl;

    /** 申请人姓名 */
    @TableField(exist = false)
    private String applicantName;

    /** 部门名称 */
    @TableField(exist = false)
    private String deptName;

    /** 状态标签 */
    @TableField(exist = false)
    private String statusLabel;

    /** 审批时间（审批日志中的 createTime） */
    @TableField(exist = false)
    private LocalDateTime approvalTime;

    /** 审批人姓名 */
    @TableField(exist = false)
    private String approverName;

    // ---- audit fields ----

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
