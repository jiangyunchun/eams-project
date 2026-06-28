package com.example.eams.transfer.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调拨详情 VO（PRD 6.7.3）
 */
@Data
public class TransferVO {

    private Long id;
    private String transferNo;
    private Long assetId;
    private String assetCode;
    private String assetName;
    private String category;
    private String specification;
    private Long fromDeptId;
    private String fromDeptName;
    private Long toDeptId;
    private String toDeptName;
    private Long toUserId;
    private String toUserName;
    private String toLocation;
    private String transferReason;
    private Integer status;
    private String statusLabel;
    private String remark;
    private Long applicantId;
    private String applicantName;
    private Integer version;
    private LocalDateTime createTime;
    private String createBy;
    private LocalDateTime updateTime;
}
