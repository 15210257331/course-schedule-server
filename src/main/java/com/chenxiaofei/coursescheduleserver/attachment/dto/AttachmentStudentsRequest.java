package com.chenxiaofei.coursescheduleserver.attachment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 修改课件关联学生参数（POST body）
 */
@Data
public class AttachmentStudentsRequest {

    @NotNull(message = "课件 id 不能为空")
    private Long id;

    /** 关联学生 id 列表（至少一个，重复自动去重） */
    private List<Long> studentIds;
}
