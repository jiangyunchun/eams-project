package com.example.eams.procurement.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商信息表
 */
@Data
@TableName("proc_supplier")
public class ProcurementSupplier {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 供应商名称（全局唯一） */
    private String supplierName;

    /** 供应商编码（全局唯一）: SUP-XXXX */
    private String supplierCode;

    /** 联系人姓名 */
    private String contactPerson;

    /** 联系人手机号 */
    private String contactPhone;

    /** 供应商地址 */
    private String address;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    // ---- transient fields ----

    /** 状态标签（非数据库字段） */
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
