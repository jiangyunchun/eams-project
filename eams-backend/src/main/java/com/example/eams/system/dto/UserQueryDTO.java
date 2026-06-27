package com.example.eams.system.dto;

import lombok.Data;

/**
 * 用户查询参数
 */
@Data
public class UserQueryDTO {
    /** 姓名模糊搜索 */
    private String realName;
    private Long deptId;
    private Integer status;
    private String phone;
    private int pageNum = 1;
    private int pageSize = 10;
}
