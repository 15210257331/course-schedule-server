package com.chenxiaofei.coursescheduleserver.backup.service;

import com.chenxiaofei.coursescheduleserver.backup.entity.BackupRecord;
import com.chenxiaofei.coursescheduleserver.backup.mapper.BackupRecordMapper;
import com.chenxiaofei.coursescheduleserver.config.BackupProperties;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动备份定时任务：每天北京时间 22:00 把全库关键表快照成 JSON 文件，保留最近 N 份，
 * 并写入 backup_record 供管理端查看。
 * 备份存储位置由配置开关决定：本地目录（app.backup.cos.enabled=false）
 * 或腾讯云 COS（=true，异地备份，本地不留存）。
 * <p>
 * 调度使用 cron 精确触发，不做补偿：若服务在 22:00 未运行（发布重启 / 停机），
 * 当天不会补备份。cron 表达式里显式声明时区，不依赖 JVM 默认时区。
 */
@Component
public class AutoBackupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoBackupScheduler.class);

    /** 全库备份涉及的业务表 */
    private static final List<String> TABLES = List.of(
            "`user`", "organization", "course", "course_template", "course_message",
            "setting", "settlement", "admin_message", "admin_message_read",
            "attachment");

    /** 保留的最近备份份数 */
    private static final int KEEP = 14;

    /** 备份时间戳所依据的时区：容器 JVM 默认时区可能是 UTC，不显式指定会与数据库差 8 小时 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private static final DateTimeFormatter NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

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

    /** 备份文件名（北京时间），手动与自动共用同一命名规则 */
    public static String newBackupName() {
        return "full-backup-" + LocalDateTime.now(ZONE).format(NAME_FORMAT) + ".json";
    }

    /** 每日自动备份：北京时间 22:00，按当前全局存储位置执行一次 */
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Shanghai")
    public void autoBackup() {
        backupOnce(newBackupName(), backupSettingService.useCos(), false);
    }

    /**
     * 执行一次备份，落地到指定存储位置（local / cos），并写入 backup_record。
     *
     * @param name         备份文件名
     * @param useCos       是否使用腾讯云 COS（true=cos，false=服务器本地）
     * @param manual       是否为管理端手动触发（记为 trigger_type=manual，仅用于区分来源）
     * @return 新写入的备份记录（含 id，供管理端前端刷新定位）
     */
    public BackupRecord backupOnce(String name, boolean useCos, boolean manual) {
        String triggerType = manual ? "manual" : "auto";
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("backupAt", LocalDateTime.now(ZONE).toString());
            snapshot.put("tables", dumpTables());

            BackupRecord record = new BackupRecord();
            record.setFileName(name);
            record.setStatus("success");
            record.setTriggerType(triggerType);

            if (useCos) {
                // 异地备份：先写临时文件上传到 COS，成功后删除本地临时文件
                Files.createDirectories(localDir);
                Path tmp = localDir.resolve(".tmp-" + name);
                try {
                    objectMapper.writeValue(tmp.toFile(), snapshot);
                    // 临时文件上传后即删除，大小必须在这之前取
                    record.setFileSize(Files.size(tmp));
                    String key = cosStorageService.upload(tmp.toFile(), name);
                    record.setStorageType("cos");
                    record.setFilePath(key);
                } finally {
                    Files.deleteIfExists(tmp);
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
            record.setTriggerType(triggerType);
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