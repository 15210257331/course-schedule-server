package com.chenxiaofei.coursescheduleserver.common;

import lombok.Data;

/**
 * 通用分页条数请求体：替换各处 {@code @RequestBody Map<String,Integer> body} + {@code body.get("limit")}。
 */
@Data
public class LimitRequest {

    /** 取数上限，默认 20 */
    private Integer limit = 20;
}
