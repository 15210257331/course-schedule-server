package com.chenxiaofei.coursescheduleserver.student.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentRequest {

    @NotBlank(message = "学生标题不能为空")
    private String title;

    @NotBlank(message = "学生姓名不能为空")
    private String studentName;
    private Long organizationId;
    private String subject;
    private String stage;
    private String courseType;

    @NotNull(message = "默认时长不能为空")
    @Min(value = 15, message = "默认时长至少 15 分钟")
    private Integer durationMinutes;

    private BigDecimal fee;
    private String location;
    private String note;
    /** 拖入日历时的重复规则：daily/weekly，NULL 不重复 */
    private String repeatType;
    /** 编辑学生时是否同步到已排课程 */
    private Boolean syncCourses;
}
