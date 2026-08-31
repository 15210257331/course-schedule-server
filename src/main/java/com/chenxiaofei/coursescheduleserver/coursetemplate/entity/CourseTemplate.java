package com.chenxiaofei.coursescheduleserver.coursetemplate.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseTemplate {

    private Long id;
    private Long userId;
    private String title;
    private Long studentId;
    private String studentName;
    private Long organizationId;
    private String subject;
    /** 学段（初一/初二/初三/高一/高二/高三） */
    private String stage;
    private String courseType;
    private Integer durationMinutes;
    private BigDecimal fee;
    private Boolean feeManual;
    private String location;
    private String note;
    private String color;
    /** 拖入日历时的重复规则：daily/weekly，NULL 不重复（daily 排到本月底，weekly 排到本月末） */
    private String repeatType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 联表冗余字段 */
    private String organizationName;
}
