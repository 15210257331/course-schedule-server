package com.chenxiaofei.coursescheduleserver.attachment.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 附件：可挂到业务对象（课程模板）或归入用户自定义分组（附件管理页）的资源文件元数据。
 */
@Data
public class Attachment {

    private Long id;
    private Long userId;
    /** 附件分组 id（附件管理页自定义分组），未分组为 null */
    private Long groupId;
    /** 业务类型：template / general（预留扩展） */
    private String bizType;
    /** 业务对象 id（模板 id / 0 表示未关联） */
    private Long bizId;
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
