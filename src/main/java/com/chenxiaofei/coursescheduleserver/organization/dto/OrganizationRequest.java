package com.chenxiaofei.coursescheduleserver.organization.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrganizationRequest {

    @NotBlank(message = "机构名称不能为空")
    private String name;

    @NotBlank(message = "机构地址不能为空")
    private String address;

    private String color;
    private String remark;
}
