package com.chenxiaofei.coursescheduleserver.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrganizationRequest {

    @NotBlank(message = "机构名称不能为空")
    private String name;

    private String contactName;
    private String contactPhone;
    private String address;
    private BigDecimal defaultFee;
    private String color;
    private String remark;
}