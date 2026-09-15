package com.chenxiaofei.coursescheduleserver.backup.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 备份恢复：把 {@link AutoBackupScheduler} 导出的备份 JSON（{backupAt, tables:{表名:[行]}}）
 * 全量还原回数据库。策略为「先清空再插入」—— 对每张业务表 DELETE 后按 JSON 行原样插回，
 * 保留原 id 与外键关系（course.user_id / course.template_id 等）。
 * <p>
 * 不恢复 {@code user} 表（避免覆盖/锁死当前管理员账号；同一服务器恢复场景下用户行不变，
 * 课程等数据的 user_id 引用仍有效）。已废弃表（notification / attachment_group）按当前库
 * schema 做列交集后跳过，不报错。
 */
@Service
public class BackupRestoreService {

    private static final Logger log = LoggerFactory.getLogger(BackupRestoreService.class);

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public BackupRestoreService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    /**
     * 导入备份 JSON 恢复数据库。
     * 整个过程在单事务内，任一表失败则全部回滚，原数据不变。
     *
     * @param file 上传的备份 JSON 文件
     * @return tables=已恢复表名列表，rows=恢复总行数，skipped=跳过的表名列表
     */
    @Transactional
    public Map<String, Object> restore(MultipartFile file) {
        Map<String, Object> snapshot = parse(file);
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> tables = (Map<String, List<Map<String, Object>>>) snapshot.get("tables");
        if (tables == null || tables.isEmpty()) {
            throw new BusinessException(400, "备份文件无 tables 数据");
        }

        // 当前库 schema：表名 -> (列名 -> DATA_TYPE)
        Map<String, Map<String, String>> schema = loadCurrentColumns();

        List<String> restored = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        long totalRows = 0;

        for (Map.Entry<String, List<Map<String, Object>>> e : tables.entrySet()) {
            String table = stripBackticks(e.getKey());
            if ("user".equalsIgnoreCase(table)) {
                // 账号表保留不清空，避免覆盖当前登录态/管理员
                continue;
            }
            Map<String, String> columns = schema.get(table);
            if (columns == null) {
                // 已废弃表（notification / attachment_group 等），跳过
                skipped.add(table);
                continue;
            }
            List<Map<String, Object>> rows = e.getValue();
            if (rows == null || rows.isEmpty()) {
                // 清空但无行可插
                jdbc.execute("DELETE FROM `" + table + "`");
                restored.add(table);
                continue;
            }
            // 取 JSON 行字段与当前表列的交集（自动丢弃旧 biz_type/group_id 等已删列）
            LinkedHashSet<String> cols = intersectColumns(rows.get(0).keySet(), columns.keySet());
            if (cols.isEmpty()) {
                skipped.add(table);
                continue;
            }
            jdbc.execute("DELETE FROM `" + table + "`");
            for (Map<String, Object> row : rows) {
                insertRow(table, cols, row, columns);
                totalRows++;
            }
            restored.add(table);
            log.info("恢复表 {}：{} 行", table, rows.size());
        }

        log.info("备份恢复完成：共 {} 张表 {} 行，跳过 {}", restored.size(), totalRows, skipped);
        return Map.of(
                "tables", restored,
                "rows", totalRows,
                "skipped", skipped
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(MultipartFile file) {
        try {
            return objectMapper.readValue(file.getInputStream(), Map.class);
        } catch (Exception ex) {
            log.warn("备份文件解析失败：{}", ex.getMessage());
            throw new BusinessException(400, "备份文件格式无效：" + ex.getMessage());
        }
    }

    /** 查 INFORMATION_SCHEMA 拿当前库每张表的列与 DATA_TYPE */
    private Map<String, Map<String, String>> loadCurrentColumns() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE()");
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String table = String.valueOf(r.get("TABLE_NAME"));
            String col = String.valueOf(r.get("COLUMN_NAME"));
            String type = String.valueOf(r.get("DATA_TYPE")).toLowerCase();
            result.computeIfAbsent(table, k -> new LinkedHashMap<>()).put(col, type);
        }
        return result;
    }

    /** JSON 行字段名与当前表列名做交集，保持当前表列顺序 */
    private LinkedHashSet<String> intersectColumns(Set<String> jsonKeys, Set<String> tableCols) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String c : tableCols) {
            for (String j : jsonKeys) {
                if (c.equalsIgnoreCase(j)) {
                    result.add(c);
                    break;
                }
            }
        }
        return result;
    }

    /** 按行列交集构造 INSERT 并执行，按 DATA_TYPE 转换时间值 */
    private void insertRow(String table, LinkedHashSet<String> cols, Map<String, Object> row,
                           Map<String, String> columnTypes) {
        List<String> colList = new ArrayList<>(cols);
        List<Object> values = new ArrayList<>(colList.size());
        for (String c : colList) {
            Object v = lookup(row, c);
            String type = columnTypes.get(c);
            values.add(convert(v, type));
        }
        String sql = "INSERT INTO `" + table + "` (" +
                colList.stream().map(c -> "`" + c + "`").reduce((a, b) -> a + ", " + b).orElse("") +
                ") VALUES (" +
                colList.stream().map(c -> "?").reduce((a, b) -> a + ", " + b).orElse("") + ")";
        jdbc.update(sql, values.toArray());
    }

    /** 大小写不敏感地按列名取 JSON 行字段值 */
    private Object lookup(Map<String, Object> row, String col) {
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (col.equalsIgnoreCase(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /** datetime/timestamp/date 按字符串解析为 java 时间类型，其余原样 */
    private Object convert(Object v, String dataType) {
        if (v == null) {
            return null;
        }
        if (dataType == null) {
            return v;
        }
        if ("datetime".equals(dataType) || "timestamp".equals(dataType)) {
            return v instanceof LocalDateTime ? v : parseDateTime(String.valueOf(v));
        }
        if ("date".equals(dataType)) {
            return v instanceof LocalDate ? v : LocalDate.parse(String.valueOf(v));
        }
        return v;
    }

    /** 兼容备份里 LocalDateTime 序化成的 ISO 或带 T 的字符串 */
    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignored) {
            // 形如 2026-09-11 18:38:30（空格分隔）
            return LocalDateTime.parse(s.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }

    private String stripBackticks(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("`", "").trim();
    }
}
