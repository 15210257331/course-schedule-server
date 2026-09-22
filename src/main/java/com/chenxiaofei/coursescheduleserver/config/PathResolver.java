package com.chenxiaofei.coursescheduleserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 目录路径解析：集中把配置中的相对目录字符串规范为绝对 Path，消除各处 @PostConstruct 重复推导。
 */
@Component
public class PathResolver {

    private final Path uploadDir;
    private final Path backupDir;

    public PathResolver(@Value("${app.upload-dir:./uploads}") String uploadDir, BackupProperties backupProperties) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.backupDir = Paths.get(backupProperties.getLocalDir()).toAbsolutePath().normalize();
    }

    /** 上传文件根目录（附件、头像） */
    public Path uploadDir() {
        return uploadDir;
    }

    /** 备份文件本地目录 */
    public Path backupDir() {
        return backupDir;
    }
}
