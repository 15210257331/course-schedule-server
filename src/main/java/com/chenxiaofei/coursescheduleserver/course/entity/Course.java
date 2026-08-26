package com.chenxiaofei.coursescheduleserver.course.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Course {

    private Long id;
    private Long userId;
    private String title;
    private Long studentId;
    private Long organizationId;
    private String subject;
    private String courseType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal fee;
    private Boolean feeManual;
    private String location;
    private String note;
    private String status;
    private String color;
    private Integer reminderOffsetMinutes;
    private String repeatType;
    private LocalDate repeatEndDate;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String studentName;
    private String organizationName;
}