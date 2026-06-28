package com.example.eams.procurement.dto;

import lombok.Data;

/**
 * 供应商查询请求
 */
@Data
public class SupplierQueryDTO {

    /** 供应商名称（模糊搜索） */
    private String supplierName;

    /** 联系人（模糊搜索） */
    private String contactPerson;

    /** 联系电话（模糊搜索） */
    private String contactPhone;

    private int pageNum = 1;

    private int pageSize = 10;
}
