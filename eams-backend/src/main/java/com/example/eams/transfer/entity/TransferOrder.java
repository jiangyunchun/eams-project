package com.example.eams.transfer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产调拨申请单表（PRD 6.7）
 * <p>
 * status: 0-待调入确认, 1-待资产管理员审批, 2-已通过, 3-已驳回
 */
@Data
@TableName("trans_order")
public class TransferOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 调拨编号（全局唯一）: DB-YYYYMMDD-XXXX */
    private String transferNo;

    /** 调拨资产ID (FK -> asset_info.id) */
    private Long assetId;

    /** 调出部门ID */
    private Long fromDeptId;

    /** 调入部门ID */
    private Long toDeptId;

    /** 调入后使用人ID */
    private Long toUserId;

    /** 调入后存放地点 */
    private String toLocation;

    /** 调拨原因: 10-500字符 */
    private String transferReason;

    /** 调拨状态: 0-待调入确认, 1-待资产管理员审批, 2-已通过, 3-已驳回 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 调拨申请人ID */
    private Long applicantId;

    /** 乐观锁版本号 */
    private Integer version;

    // ---- transient fields (not in DB) ----

    @TableField(exist = false)
    private String assetCode;
    @TableField(exist = false)
    private String assetName;
    @TableField(exist = false)
    private String category;
    @TableField(exist = false)
    private String specification;
    @TableField(exist = false)
    private String imageUrl;
    @TableField(exist = false)
    private String fromDeptName;
    @TableField(exist = false)
    private String toDeptName;
    @TableField(exist = false)
    private String toUserName;
    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String statusLabel;

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
