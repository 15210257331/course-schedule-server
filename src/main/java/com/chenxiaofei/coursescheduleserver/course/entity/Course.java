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
    private String studentName;
    private Long organizationId;
    private String subject;
    /** 学段（初一/初二/初三/高一/高二/高三），冗余自课程模板 */
    private String stage;
    private String courseType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal fee;
    private String location;
    private String note;
    private String status;
    private String color;
    private Integer reminderOffsetMinutes;
    private String repeatType;
    private LocalDate repeatEndDate;
    private Long parentId;
    /** 排课来源模板 id（冗余，用于删除模板时级联删除、编辑模板时同步课程） */
    private Long templateId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 联表冗余：机构名（学生名走存储列 studentName，见上） */
    private String organizationName;
}