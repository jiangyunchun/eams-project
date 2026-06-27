package com.example.eams.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果
 *
 * @param <T> 列表项类型
 */
@Data
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页码 */
    private int pageNum;

    /** 每页条数 */
    private int pageSize;

    /** 总页数 */
    private int pages;

    /** 数据列表 */
    private List<T> list;

    /**
     * 从 MyBatis-Plus 分页对象转换
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPageNum((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        result.setPages((int) page.getPages());
        result.setList(page.getRecords());
        return result;
    }

    /**
     * 空分页结果
     */
    public static <T> PageResult<T> empty(int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(0);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages(0);
        result.setList(Collections.emptyList());
        return result;
    }
}
