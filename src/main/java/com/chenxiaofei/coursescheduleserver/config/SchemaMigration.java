package com.chenxiaofei.coursescheduleserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 轻量启动迁移：对「CREATE TABLE IF NOT EXISTS」无法覆盖的存量表做幂等列补齐。
 * 用于给 course / course_template 增加 student_name 列（学生改为自由文本后的存储字段）。
 */
@Component
@Order(0)
public class SchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbc;

    public SchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        addColumnIfMissing("course_template", "student_name",
                "ALTER TABLE course_template ADD COLUMN student_name VARCHAR(50) NULL AFTER student_id");
        addColumnIfMissing("course", "student_name",
                "ALTER TABLE course ADD COLUMN student_name VARCHAR(50) NULL AFTER student_id");
        addColumnIfMissing("organization", "color",
                "ALTER TABLE organization ADD COLUMN color VARCHAR(20) NULL AFTER default_fee");
        addColumnIfMissing("user", "subjects",
                "ALTER TABLE `user` ADD COLUMN subjects VARCHAR(200) NULL AFTER phone");
        addColumnIfMissing("course_template", "stage",
                "ALTER TABLE course_template ADD COLUMN stage VARCHAR(20) NULL AFTER subject");
        addColumnIfMissing("course", "stage",
                "ALTER TABLE course ADD COLUMN stage VARCHAR(20) NULL AFTER subject");
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        List<String> cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class, table, column);
        if (!cols.isEmpty()) {
            return;
        }
        try {
            jdbc.execute(ddl);
            log.info("迁移完成：{}.{} 列已添加", table, column);
        } catch (Exception e) {
            // 表还不存在时由 schema.sql 的 CREATE TABLE 建出完整结构，此处静默跳过
            log.warn("迁移跳过（{}.{})：{}", table, column, e.getMessage());
        }
    }
}
