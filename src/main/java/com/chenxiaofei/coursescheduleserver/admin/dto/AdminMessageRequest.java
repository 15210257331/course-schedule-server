package com.chenxiaofei.coursescheduleserver.admin.dto;

import lombok.Data;

/**
 * 消息创建/更新请求
 */
@Data
public class AdminMessageRequest {

    private Long id;

    private String title;

    private String content;

    /** announcement/activity/notice，默认 announcement */
    private String type;

    /** all/specific，默认 all */
    private String targetType;

    /** targetType=specific 时的教师ID列表 */
    private java.util.List<Long> targetIds;
}
