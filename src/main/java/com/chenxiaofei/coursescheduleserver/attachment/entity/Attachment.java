package com.chenxiaofei.coursescheduleserver.attachment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件：挂在课程模板（学生）下的资源文件元数据。
 */
@Data
public class Attachment {

    private Long id;
    private Long userId;
    /** 所属课程模板 id（一个模板代表一个学生） */
    private Long templateId;
    /** 原始文件名（展示与下载用） */
    private String fileName;
    /** 相对路径 /uploads/attachment/... */
    private String filePath;
    /** 字节数 */
    private Long fileSize;
    /** MIME 类型（可空） */
    private String mimeType;
    private LocalDateTime createdAt;
}
