package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.backup.entity.BackupRecord;
import com.chenxiaofei.coursescheduleserver.backup.mapper.BackupRecordMapper;
import com.chenxiaofei.coursescheduleserver.backup.service.AutoBackupScheduler;
import com.chenxiaofei.coursescheduleserver.backup.service.BackupRestoreService;
import com.chenxiaofei.coursescheduleserver.backup.service.BackupSettingService;
import com.chenxiaofei.coursescheduleserver.backup.service.CosStorageService;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.IdRequest;
import com.chenxiaofei.coursescheduleserver.common.LimitRequest;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.config.PathResolver;
import com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 管理端 - 自动备份记录
 */
@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
public class AdminBackupController {

    private final BackupRecordMapper backupRecordMapper;
    private final CosStorageService cosStorageService;
    private final BackupSettingService backupSettingService;
    private final AutoBackupScheduler autoBackupScheduler;
    private final BackupRestoreService backupRestoreService;
    private final PathResolver pathResolver;

    /** 最近 N 条备份记录 */
    @PostMapping("/recent")
    public Result<List<BackupRecord>> recent(@RequestBody(required = false) LimitRequest body) {
        int limit = (body != null && body.getLimit() != null) ? body.getLimit() : 20;
        limit = Math.min(Math.max(limit, 1), 100);
        return Result.ok(backupRecordMapper.listRecent(limit));
    }

    /** 全局备份存储位置（cos / local） */
    @PostMapping("/storage-setting")
    public Result<Map<String, Object>> storageSetting() {
        String storageType = backupSettingService.storageType();
        return Result.ok(Map.of("storageType", storageType));
    }

    /** 更新全局备份存储位置（运行时生效，供管理端开关） */
    @PostMapping("/storage-setting/save")
    @OperationLog(module = "backup", action = "SET_STORAGE", detail = "{#body['storageType']}")
    public Result<Map<String, Object>> saveStorageSetting(@RequestBody Map<String, String> body) {
        String storageType = body == null ? null : body.get("storageType");
        backupSettingService.setStorageType(storageType);
        return Result.ok(Map.of("storageType", backupSettingService.storageType()));
    }

    /**
     * 立即手动触发一次备份：按当前全局存储位置（即顶部「存储位置」开关所选）
     * 进行备份，与每日 22:00 的定时备份使用同一存储位置。切换位置后立即生效。
     * 返回新写入的备份记录（含 id），供前端刷新定位。
     */
    @PostMapping("/run")
    @OperationLog(module = "backup", action = "RUN_NOW")
    public Result<BackupRecord> runNow() {
        BackupRecord record = autoBackupScheduler.backupOnce(
                AutoBackupScheduler.newBackupName(), backupSettingService.useCos(), true);
        if (!"success".equals(record.getStatus())) {
            throw new BusinessException(500, "备份失败：" + record.getErrorMsg());
        }
        return Result.ok(record);
    }

    /**
     * 下载备份文件：
     * - local：直接返回服务器本地文件流；
     * - cos：返回 302 跳转到 COS 临时签名 URL（仅管理员可触发）。
     */
    @PostMapping("/download")
    public Object download(@RequestBody IdRequest body) {
        Long id = body.getId();
        BackupRecord record = backupRecordMapper.getById(id);
        if (record == null) {
            throw new BusinessException(404, "备份记录不存在");
        }
        if (!"success".equals(record.getStatus())) {
            throw new BusinessException(400, "该备份失败，无文件可下载");
        }

        if ("cos".equals(record.getStorageType())) {
            String url = cosStorageService.presignUrl(record.getFilePath(), 10);
            return ResponseEntity.status(302)
                    .header(HttpHeaders.LOCATION, url)
                    .build();
        }

        // 本地存储：filePath 形如 /backup/full-backup-xxx.json，映射到本地目录
        String fileName = record.getFileName();
        Path file = pathResolver.backupDir().resolve(fileName);
        if (!Files.exists(file)) {
            throw new BusinessException(404, "备份文件已不存在（可能已被清理）");
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(resource);
    }

    /**
     * 导入历史备份 JSON 恢复数据库：清空当前业务表（课程 / 模板 / 机构等，教师账号 user 表保留）
     * 后按 JSON 全量插回，保留原 id 与外键关系。单事务，失败回滚。
     */
    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @OperationLog(module = "backup", action = "RESTORE")
    public Result<Map<String, Object>> restore(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择备份文件");
        }
        return Result.ok(backupRestoreService.restore(file));
    }
}
