package com.chenxiaofei.coursescheduleserver.attachment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课件：资源文件元数据，通过 attachment_student 关联表挂到多个学生下。
 */
@Data
public class Attachment {

    private Long id;
    private Long userId;
    /** 原始文件名（展示与下载用） */
    private String fileName;
    /** 相对路径 /uploads/attachment/... */
    private String filePath;
    /** 字节数 */
    private Long fileSize;
    /** MIME 类型（可空） */
    private String mimeType;
    private LocalDateTime createdAt;

    /** 联表冗余：关联的学生姓名（顿号拼接，课件管理列表用） */
    private String studentName;
    /** 联表冗余：关联的学生 id 逗号串（前端 split 后使用） */
    private String studentIds;
}
