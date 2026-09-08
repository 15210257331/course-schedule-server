package com.chenxiaofei.coursescheduleserver.attachmentgroup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 附件分组创建 / 更新请求。
 */
@Data
public class AttachmentGroupRequest {

    @NotBlank(message = "分组名称不能为空")
    private String name;
}