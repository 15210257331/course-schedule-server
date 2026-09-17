package com.chenxiaofei.coursescheduleserver.backup.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自动备份记录：定时任务生成备份文件时写入，供管理端查看备份历史。
 */
@Data
public class BackupRecord {

    private Long id;
    private String fileName;
    private String filePath;
    private Long fileSize;
    /** 状态：success/failed */
    private String status;
    /** 存储位置：local（服务器本地）/ cos（腾讯云 COS） */
    private String storageType;
    /** 触发方式：auto（每日定时）/ manual（管理端手动）；历史数据为空 */
    private String triggerType;
    private String errorMsg;
    private LocalDateTime createdAt;
}
