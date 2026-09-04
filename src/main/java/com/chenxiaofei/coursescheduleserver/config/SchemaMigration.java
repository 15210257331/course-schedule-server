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
        addColumnIfMissing("user", "status",
                "ALTER TABLE `user` ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER role");
        addColumnIfMissing("user", "disabled_reason",
                "ALTER TABLE `user` ADD COLUMN disabled_reason VARCHAR(500) NULL AFTER status");
        addColumnIfMissing("user", "disabled_at",
                "ALTER TABLE `user` ADD COLUMN disabled_at DATETIME NULL AFTER disabled_reason");
        addColumnIfMissing("user", "last_login_at",
                "ALTER TABLE `user` ADD COLUMN last_login_at DATETIME NULL AFTER disabled_at");
        migrateCourseType();
        createAdminTables();
    }

    /** 管理端消息表（CREATE TABLE IF NOT EXISTS，幂等） */
    private void createAdminTables() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS admin_message (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "title VARCHAR(200) NOT NULL COMMENT '消息标题'," +
                "content TEXT NOT NULL COMMENT '消息内容'," +
                "type VARCHAR(20) NOT NULL DEFAULT 'announcement' COMMENT '类型：announcement/activity/notice'," +
                "target_type VARCHAR(20) NOT NULL DEFAULT 'all' COMMENT '目标：all/specific'," +
                "target_ids VARCHAR(2000) COMMENT '目标教师ID列表，逗号分隔'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'published' COMMENT '状态：published/revoked'," +
                "created_by BIGINT NOT NULL COMMENT '发布管理员ID'," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_msg_status (status)," +
                "INDEX idx_msg_created (created_at)" +
                ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS admin_message_read (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "message_id BIGINT NOT NULL," +
                "user_id BIGINT NOT NULL," +
                "read_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_msg_user (message_id, user_id)" +
                ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
    }

    /**
     * 课程类型统一为「一对一 / 家教 / 班课」三类，迁移历史旧值（幂等）：
     * 家教版→家教，小班课/大班课→班课，试听→一对一。
     */
    private void migrateCourseType() {
        String[][] tables = {{"course", "course_type"}, {"course_template", "course_type"}};
        for (String[] t : tables) {
            jdbc.update("UPDATE " + t[0] + " SET " + t[1] + " = '家教' WHERE " + t[1] + " = '家教版'");
            jdbc.update("UPDATE " + t[0] + " SET " + t[1] + " = '班课' WHERE " + t[1] + " IN ('小班课', '大班课')");
            jdbc.update("UPDATE " + t[0] + " SET " + t[1] + " = '一对一' WHERE " + t[1] + " = '试听'");
        }
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
