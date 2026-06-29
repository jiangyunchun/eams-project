package com.example.eams.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 资产报修单表（PRD 6.8）
 * repair_status: 0-待维修, 1-维修中, 2-已修复, 3-无法修复
 */
@Data
@TableName("repair_order")
public class RepairOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报修编号 WX-YYYYMMDD-XXXX */
    private String repairNo;
    private Long assetId;
    private Long applicantId;
    /** 故障类型: 硬件故障/软件故障/网络故障/配件更换/其他 */
    private String faultType;
    /** 紧急程度: 0-普通, 1-紧急 */
    private Integer urgency;
    /** 故障描述 10-500字符 */
    private String faultDesc;
    /** 故障图片URL逗号分隔 */
    private String faultImages;
    private String contactPhone;
    /** 维修状态: 0-待维修,1-维修中,2-已修复,3-无法修复 */
    private Integer repairStatus;
    /** 维修前资产状态快照 */
    private Integer preRepairStatus;
    private String remark;

    // ---- transient ----
    @TableField(exist = false) private String assetCode;
    @TableField(exist = false) private String assetName;
    @TableField(exist = false) private String category;
    @TableField(exist = false) private String specification;
    @TableField(exist = false) private String location;
    @TableField(exist = false) private String imageUrl;
    @TableField(exist = false) private String deptName;
    @TableField(exist = false) private String applicantName;
    @TableField(exist = false) private String statusLabel;
    @TableField(exist = false) private String urgencyLabel;
    // repair_record transient fields
    @TableField(exist = false) private String repairMethod;
    @TableField(exist = false) private String repairPerson;
    @TableField(exist = false) private java.math.BigDecimal repairFee;
    @TableField(exist = false) private LocalDateTime startDate;
    @TableField(exist = false) private LocalDateTime finishDate;
    @TableField(exist = false) private String faultReason;
    @TableField(exist = false) private String solution;
    @TableField(exist = false) private String repairFiles;

    // ---- audit ----
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT)
    private String createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
    @TableLogic @TableField("is_deleted")
    private Integer isDeleted;
}
