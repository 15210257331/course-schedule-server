package com.chenxiaofei.coursescheduleserver.salaryrule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryRuleRequest {

    private Long organizationId;
    private String grade;
    private String subject;

    @NotNull(message = "课时费不能为空")
    private BigDecimal hourlyFee;

    private String remark;
}