package com.chenxiaofei.coursescheduleserver.backup.service;

import com.chenxiaofei.coursescheduleserver.backup.entity.BackupRecord;
import com.chenxiaofei.coursescheduleserver.backup.mapper.BackupRecordMapper;
import com.chenxiaofei.coursescheduleserver.config.BackupProperties;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动备份定时任务：每天晚上 22:00 把全库关键表快照成 JSON 文件，保留最近 N 份，
 * 并写入 backup_record 供管理端查看。
 * 备份存储位置由配置开关决定：本地目录（app.backup.cos.enabled=false）
 * 或腾讯云 COS（=true，异地备份，本地不留存）。
 * <p>
 * 调度方式为短周期轮询（每 30 秒一次）：若服务在备份时刻未运行
 * （如夜间停机后重启），启动后也会自动补一次当天的备份。
 */
@Component
public class AutoBackupScheduler implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AutoBackupScheduler.class);

    /** 全库备份涉及的业务表 */
    private static final List<String> TABLES = List.of(
            "`user`", "organization", "course", "course_template", "course_message",
            "setting", "settlement", "admin_message", "admin_message_read",
            "attachment");

    /** 保留的最近备份份数 */
    private static final int KEEP = 14;

    /** 每日自动备份时刻：晚上 22:00 */
    private static final LocalTime BACKUP_TIME = LocalTime.of(22, 0);

    private final JdbcTemplate jdbc;
    private final BackupRecordMapper backupRecordMapper;
    private final ObjectMapper objectMapper;
    private final BackupProperties backupProperties;
    private final CosStorageService cosStorageService;
    private final BackupSettingService backupSettingService;
    private final Path localDir;

    public AutoBackupScheduler(JdbcTemplate jdbc,
                               BackupRecordMapper backupRecordMapper,
                               ObjectMapper objectMapper,
                               BackupProperties backupProperties,
                               CosStorageService cosStorageService,
                               BackupSettingService backupSettingService) {
        this.jdbc = jdbc;
        this.backupRecordMapper = backupRecordMapper;
        this.objectMapper = objectMapper;
        this.backupProperties = backupProperties;
        this.cosStorageService = cosStorageService;
        this.backupSettingService = backupSettingService;
        // 备份本地目录：独立于 uploads，避免被 /uploads/** 静态映射公开访问
        this.localDir = Paths.get(backupProperties.getLocalDir()).toAbsolutePath().normalize();
    }

    /** 最近一次备份日期（null 表示尚未从备份记录回填），用于保证每天最多备份一次 */
    private LocalDate lastBackupDay;

    // 轮询注册：每 30 秒检查一次是否到达每日备份时间
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addFixedDelayTask(this::backupTick, Duration.ofSeconds(30));
    }

    /** 到达每日备份时间（22:00）且今天尚未备份时执行备份 */
    private synchronized void backupTick() {
        LocalDate today = LocalDate.now();
        if (lastBackupDay == null) {
            // 启动后首次检查：今天已有备份记录则跳过，避免重启后重复备份
            lastBackupDay = latestBackupDay();
        }
        if (today.equals(lastBackupDay)) {
            return;
        }
        if (LocalTime.now().isBefore(BACKUP_TIME)) {
            return;
        }
        lastBackupDay = today;
        autoBackup();
    }

    /** 最近一条备份记录所属日期，读取失败时返回 null（视为今天尚未备份，宁可多备不可漏备） */
    private LocalDate latestBackupDay() {
        try {
            List<BackupRecord> latest = backupRecordMapper.listRecent(1);
            return latest.isEmpty() ? null : latest.get(0).getCreatedAt().toLocalDate();
        } catch (Exception e) {
            log.warn("读取最近备份记录失败：{}", e.getMessage());
            return null;
        }
    }

    /** 执行自动备份（每天晚上 22:00 触发） */
    public void autoBackup() {
        String name = "full-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        backupOnce(name, backupSettingService.useCos(), false);
    }

    /**
     * 执行一次备份，落地到指定存储位置（local / cos），并写入 backup_record。
     *
     * @param name         备份文件名
     * @param useCos       是否使用腾讯云 COS（true=cos，false=服务器本地）
     * @param manual       是否为管理端手动触发（用于日志区分，不改变备份逻辑）
     * @return 新写入的备份记录（含 id，供管理端前端刷新定位）
     */
    public BackupRecord backupOnce(String name, boolean useCos, boolean manual) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("backupAt", LocalDateTime.now().toString());
            snapshot.put("tables", dumpTables());

            BackupRecord record = new BackupRecord();
            record.setFileName(name);
            record.setStatus("success");

            if (useCos) {
                // 异地备份：先写临时文件上传到 COS，成功后删除本地临时文件
                File tmp = Files.createFile(localDir.resolve(".tmp-" + name)).toFile();
                try {
                    objectMapper.writeValue(tmp, snapshot);
                    String key = cosStorageService.upload(tmp);
                    record.setStorageType("cos");
                    record.setFilePath(key);
                } finally {
                    Files.deleteIfExists(tmp.toPath());
                }
            } else {
                Files.createDirectories(localDir);
                Path file = localDir.resolve(name);
                objectMapper.writeValue(file.toFile(), snapshot);
                record.setStorageType("local");
                record.setFilePath("/backup/" + name);
                record.setFileSize(Files.size(file));
                prune();
            }

            backupRecordMapper.insert(record);
            log.info("{}备份完成：{}（{}）", manual ? "手动" : "自动", name, record.getStorageType());
            return record;
        } catch (Exception e) {
            log.error("{}备份失败：{}", manual ? "手动" : "自动", e.getMessage(), e);
            BackupRecord record = new BackupRecord();
            record.setFileName(name);
            record.setFilePath(null);
            record.setStatus("failed");
            record.setStorageType(useCos ? "cos" : "local");
            record.setErrorMsg(e.getMessage());
            try {
                backupRecordMapper.insert(record);
            } catch (Exception ex) {
                log.error("备份失败记录写入失败：{}", ex.getMessage());
            }
            return record;
        }
    }

    private Map<String, List<Map<String, Object>>> dumpTables() {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        for (String table : TABLES) {
            try {
                List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + table);
                result.put(table, rows);
            } catch (Exception e) {
                log.warn("备份表 {} 失败：{}", table, e.getMessage());
                result.put(table, List.of());
            }
        }
        return result;
    }

    /** 只保留最近 KEEP 份备份文件（仅本地存储时生效） */
    private void prune() {
        try {
            File[] files = localDir.toFile().listFiles((dir, name) -> name.startsWith("full-backup-") && name.endsWith(".json"));
            if (files == null || files.length <= KEEP) {
                return;
            }
            java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
            for (int i = KEEP; i < files.length; i++) {
                Files.deleteIfExists(files[i].toPath());
            }
        } catch (Exception e) {
            log.warn("备份清理失败：{}", e.getMessage());
        }
    }
}