package com.chenxiaofei.coursescheduleserver.student.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Student {

    private Long id;
    private Long userId;
    private String name;
    private String gender;
    private String grade;
    private String subject;
    private Long organizationId;
    private String phone;
    private String parentPhone;
    private BigDecimal fee;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String organizationName;
}