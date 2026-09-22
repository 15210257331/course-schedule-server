package com.chenxiaofei.coursescheduleserver.common;

import lombok.Data;

/**
 * 分页查询参数基类：统一 pageNum / pageSize 字段与默认值，子类只需声明业务筛选字段。
 */
@Data
public abstract class PageRequest {

    /** 页码，从 1 开始，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 20 */
    private Integer pageSize = 20;
}
