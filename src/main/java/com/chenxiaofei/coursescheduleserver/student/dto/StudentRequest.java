package com.chenxiaofei.coursescheduleserver.student.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentRequest {

    @NotBlank(message = "学生姓名不能为空")
    private String name;

    private String gender;
    private String grade;
    private String subject;
    private Long organizationId;
    private String phone;
    private String parentPhone;
    private BigDecimal fee;
    private String remark;
}