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
    private Long organizationId;
    private String subject;
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

    /** 复制下周：为目标周数（1 表示下周） */
    private Integer copyToWeeks;
}