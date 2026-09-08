package com.chenxiaofei.coursescheduleserver.coursetemplate.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseTemplate {

    private Long id;
    private Long userId;
    private String title;
    private String studentName;
    private Long organizationId;
    private String subject;
    /** 学段（初一/初二/初三/高一/高二/高三） */
    private String stage;
    private String courseType;
    private Integer durationMinutes;
    private BigDecimal fee;
    private String location;
    private String note;
    private String repeatType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 联表冗余字段 */
    private String organizationName;

    /** 附件数量（联表子查询，仅列表返回，用于标识是否有附件） */
    private Integer attachmentCount;
}
