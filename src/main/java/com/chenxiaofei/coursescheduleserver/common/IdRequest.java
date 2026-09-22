package com.chenxiaofei.coursescheduleserver.common;

import lombok.Data;

/**
 * 通用 ID 请求体：替换各处 {@code @RequestBody Map<String,Long> body} + {@code body.get("id")}。
 */
@Data
public class IdRequest {

    private Long id;
}
