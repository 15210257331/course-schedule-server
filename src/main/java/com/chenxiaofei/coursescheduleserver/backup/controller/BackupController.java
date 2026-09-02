package com.chenxiaofei.coursescheduleserver.backup.controller;

import com.chenxiaofei.coursescheduleserver.backup.dto.BackupData;
import com.chenxiaofei.coursescheduleserver.backup.service.BackupService;
import com.chenxiaofei.coursescheduleserver.common.Result;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据备份：导出 / 导入 JSON。
 */
@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    /** 导出：以 JSON 附件形式下载（由 Spring 的 Jackson 消息转换器序列化） */
    @GetMapping("/export")
    public ResponseEntity<BackupData> exportData() {
        BackupData data = service.exportData();
        String filename = "teacheros-backup-" + java.time.LocalDate.now() + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(data);
    }

    /** 导入：全量替换当前用户数据 */
    @PostMapping("/import")
    public Result<Integer> importData(@RequestBody BackupData data) {
        return Result.ok(service.importData(data));
    }
}