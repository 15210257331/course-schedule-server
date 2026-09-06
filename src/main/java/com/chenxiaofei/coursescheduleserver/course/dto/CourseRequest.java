package com.chenxiaofei.coursescheduleserver.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class CourseRequest {

    @NotBlank(message = "课程标题不能为空")
    private String title;

    private Long studentId;
    private String studentName;
    private Long organizationId;
    private String subject;
    private String stage;
    private String courseType;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
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
    /** 排课来源模板 id（可选，删除模板时据此级联删除课程） */
    private Long templateId;
}