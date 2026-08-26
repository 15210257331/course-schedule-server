package com.chenxiaofei.coursescheduleserver.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MoveCourseRequest {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
}