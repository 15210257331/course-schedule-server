package com.chenxiaofei.coursescheduleserver.coursemessage.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CourseMessage {

    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long courseId;
    private LocalDateTime remindAt;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
