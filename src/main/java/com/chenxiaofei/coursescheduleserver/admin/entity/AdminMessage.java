package com.chenxiaofei.coursescheduleserver.admin.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员消息（公告/活动/通知）
 */
@Data
public class AdminMessage {

    private Long id;
    private String title;
    private String content;
    /** 类型：announcement/activity/notice */
    private String type;
    /** 目标：all/specific */
    private String targetType;
    /** 目标教师ID列表，逗号分隔（target_type=specific 时） */
    private String targetIds;
    /** 状态：published/revoked */
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- 非持久化字段（查询时填充） ----

    /** 阅读数 */
    private Integer readCount;
    /** 当前用户是否已读（教师端列表用） */
    private Boolean isRead;
    /** 目标人数 */
    private Integer targetCount;
}
