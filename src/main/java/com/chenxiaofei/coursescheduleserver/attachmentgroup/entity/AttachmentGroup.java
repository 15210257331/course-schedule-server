package com.chenxiaofei.coursescheduleserver.attachmentgroup.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件分组：用户自定义的附件归类容器。
 */
@Data
public class AttachmentGroup {

    private Long id;
    private Long userId;
    /** 分组名称 */
    private String name;
    private LocalDateTime createdAt;
}