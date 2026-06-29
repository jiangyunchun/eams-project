package com.example.eams.repair.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 维修处理记录表（PRD 6.8.2）
 */
@Data
@TableName("repair_record")
public class RepairRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long repairOrderId;
    /** 维修方式: 现场维修/送修/上门维修/远程支持 */
    private String repairMethod;
    private String repairPerson;
    private BigDecimal repairFee;
    private LocalDate startDate;
    private LocalDate finishDate;
    /** 故障根因分析 */
    private String faultReason;
    /** 处理措施 */
    private String solution;
    /** 维修附件URL逗号分隔 */
    private String repairFiles;
    private String remark;

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
