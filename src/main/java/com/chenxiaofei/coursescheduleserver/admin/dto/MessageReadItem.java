package com.chenxiaofei.coursescheduleserver.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息阅读情况明细项（管理端，单个教师的已读/未读信息）
 */
@Data
public class MessageReadItem {

    private Long id;
    private String username;
    private String nickname;
    /** 是否已读 */
    private Boolean hasRead;
    /** 阅读时间（未读为空） */
    private LocalDateTime readAt;
}
