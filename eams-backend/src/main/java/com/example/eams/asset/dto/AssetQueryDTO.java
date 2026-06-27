package com.example.eams.asset.dto;

import lombok.Data;

/**
 * 资产列表查询参数
 */
@Data
public class AssetQueryDTO {
    private String assetCode;
    private String assetName;
    private String category;
    private Integer status;
    private Long deptId;
    private String userName;
    private String location;
    private String beginPurchaseDate;
    private String endPurchaseDate;
    private int pageNum = 1;
    private int pageSize = 10;
}
