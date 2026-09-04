package com.chenxiaofei.coursescheduleserver.admin.dto;

import lombok.Data;

/**
 * 禁用/启用教师请求
 */
@Data
public class TeacherStatusRequest {

    private Long id;

    /** active / disabled */
    private String status;

    /** 禁用原因（禁用时可选） */
    private String reason;
}
