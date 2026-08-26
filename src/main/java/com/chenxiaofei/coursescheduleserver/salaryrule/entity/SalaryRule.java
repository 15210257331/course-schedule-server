package com.chenxiaofei.coursescheduleserver.salaryrule.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalaryRule {

    private Long id;
    private Long userId;
    private Long organizationId;
    private String grade;
    private String subject;
    private BigDecimal hourlyFee;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String organizationName;
}