package com.chenxiaofei.coursescheduleserver.notification.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Notification {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Long courseId;
    private LocalDateTime remindAt;
    private Boolean isRead;
    private LocalDateTime createdAt;
}