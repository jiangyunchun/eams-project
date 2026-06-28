package com.example.eams.requisition.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领用审批日志表（PRD 6.3.2）
 */
@Data
@TableName("req_approval_log")
public class RequisitionApprovalLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 领用申请单ID (FK -> req_order.id) */
    private Long requisitionId;

    /** 审批人ID (FK -> sys_user.id) */
    private Long approverId;

    /** 审批级别: 1-部门管理员, 2-资产管理员 */
    private Integer approvalLevel;

    /** 审批结果: 0-驳回, 1-通过 */
    private Integer approvalResult;

    /** 驳回原因 */
    private String rejectReason;

    // ---- transient fields ----

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
