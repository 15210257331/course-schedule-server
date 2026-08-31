package com.chenxiaofei.coursescheduleserver.common;

import lombok.Data;

import java.util.List;

/**
 * 分页查询响应：total 总数 + list 本页数据
 */
@Data
public class PageResult<T> {

    private long total;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list);
    }
}