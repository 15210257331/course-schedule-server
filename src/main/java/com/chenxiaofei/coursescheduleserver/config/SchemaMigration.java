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
 * 用于给 course / student 增加 student_name 列（学生改为自由文本后的存储字段）。
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
        // 模板 → 学生 重命名迁移必须最先执行：后续迁移与 schema 引用 student / student_id
        migrateTemplateToStudent();
        addColumnIfMissing("student", "student_name",
                "ALTER TABLE student ADD COLUMN student_name VARCHAR(50) NULL AFTER student_id");
        addColumnIfMissing("course", "student_name",
                "ALTER TABLE course ADD COLUMN student_name VARCHAR(50) NULL AFTER student_id");
        addColumnIfMissing("organization", "color",
                "ALTER TABLE organization ADD COLUMN color VARCHAR(20) NULL AFTER address");
        addColumnIfMissing("user", "subjects",
                "ALTER TABLE `user` ADD COLUMN subjects VARCHAR(200) NULL AFTER phone");
        addColumnIfMissing("student", "stage",
                "ALTER TABLE student ADD COLUMN stage VARCHAR(20) NULL AFTER subject");
        addColumnIfMissing("course", "stage",
                "ALTER TABLE course ADD COLUMN stage VARCHAR(20) NULL AFTER subject");
        addColumnIfMissing("course", "student_id",
                "ALTER TABLE course ADD COLUMN student_id BIGINT NULL AFTER parent_id");
        addColumnIfMissing("user", "status",
                "ALTER TABLE `user` ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'active' AFTER role");
        addColumnIfMissing("user", "disabled_reason",
                "ALTER TABLE `user` ADD COLUMN disabled_reason VARCHAR(500) NULL AFTER status");
        addColumnIfMissing("user", "disabled_at",
                "ALTER TABLE `user` ADD COLUMN disabled_at DATETIME NULL AFTER disabled_reason");
        addColumnIfMissing("user", "last_login_at",
                "ALTER TABLE `user` ADD COLUMN last_login_at DATETIME NULL AFTER disabled_at");
        migrateCourseType();
        migrateOrganizationContacts();
        createAdminTables();
        createAuditTables();
        dropLegacyNotificationTable();
        migrateAttachmentToStudent();
        migrateAttachmentMultiStudent();
    }

    /**
     * 「课程模板」重构为「学生」：course_template 表改名 student，
     * course.template_id / attachment.template_id 改名 student_id。
     * 幂等：新库由 schema.sql 直接建出新结构，本方法对已是新结构的库无副作用。
     */
    private void migrateTemplateToStudent() {
        List<String> oldTable = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                String.class, "course_template");
        if (!oldTable.isEmpty()) {
            List<String> newTable = jdbc.queryForList(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                    String.class, "student");
            try {
                if (newTable.isEmpty()) {
                    jdbc.execute("RENAME TABLE course_template TO student");
                    log.info("迁移完成：course_template 表已重命名为 student");
                } else {
                    // schema.sql 先于本迁移执行，会为存量库空建 student 表：
                    // 若 student 为空壳则删掉后重命名，保留 course_template 中的历史数据
                    Long oldCount = jdbc.queryForObject("SELECT COUNT(*) FROM course_template", Long.class);
                    Long newCount = jdbc.queryForObject("SELECT COUNT(*) FROM student", Long.class);
                    if (newCount != null && newCount == 0) {
                        jdbc.execute("DROP TABLE student");
                        jdbc.execute("RENAME TABLE course_template TO student");
                        log.info("迁移完成：已删除 schema.sql 空建的 student 表，course_template（{} 行）重命名为 student", oldCount);
                    } else {
                        log.warn("course_template（{} 行）与 student（{} 行）同时存在且均非空，跳过自动迁移，请人工核对数据", oldCount, newCount);
                    }
                }
            } catch (Exception e) {
                log.warn("重命名 course_template → student 失败：{}", e.getMessage());
            }
        }
        renameColumnIfMissing("course", "template_id", "student_id",
                "ALTER TABLE course RENAME COLUMN template_id TO student_id");
        renameColumnIfMissing("attachment", "template_id", "student_id",
                "ALTER TABLE attachment RENAME COLUMN template_id TO student_id");
    }

    /** 列重命名：旧列存在且新列不存在时执行（幂等） */
    private void renameColumnIfMissing(String table, String oldColumn, String newColumn, String ddl) {
        List<String> oldCol = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class, table, oldColumn);
        List<String> newCol = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class, table, newColumn);
        if (oldCol.isEmpty() || !newCol.isEmpty()) {
            return;
        }
        try {
            jdbc.execute(ddl);
            log.info("迁移完成：{}.{} 列已重命名为 {}", table, oldColumn, newColumn);
        } catch (Exception e) {
            log.warn("重命名 {}.{} → {} 失败：{}", table, oldColumn, newColumn, e.getMessage());
        }
    }

    /**
     * 机构联系人字段下线：新增/编辑不再收集联系人、联系电话，地址改为必填。
     * 幂等：列不存在时跳过；address 补空值后收紧为 NOT NULL。
     */
    private void migrateOrganizationContacts() {
        dropColumnIfExists("organization", "contact_name", "ALTER TABLE organization DROP COLUMN contact_name");
        dropColumnIfExists("organization", "contact_phone", "ALTER TABLE organization DROP COLUMN contact_phone");
        try {
            jdbc.update("UPDATE organization SET address = '' WHERE address IS NULL");
            relaxColumnNotNull("organization", "address",
                    "ALTER TABLE organization MODIFY COLUMN address VARCHAR(255) NOT NULL");
        } catch (Exception e) {
            // 表不存在时由 schema.sql 建出完整结构，此处静默跳过
            log.debug("机构地址收紧跳过：{}", e.getMessage());
        }
    }

    /** 列删除：存在时才执行（幂等） */
    private void dropColumnIfExists(String table, String column, String ddl) {
        List<String> cols = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                String.class, table, column);
        if (cols.isEmpty()) {
            return;
        }
        try {
            jdbc.execute(ddl);
            log.info("迁移完成：{}.{} 列已删除", table, column);
        } catch (Exception e) {
            log.warn("删除 {}.{} 列失败：{}", table, column, e.getMessage());
        }
    }

    /**
     * 重命名遗留表：旧 {@code notification} 表带 type 字段，已重构为
     * {@code course_message}（去掉 type，只存课程提醒）。旧表与数据不再保留，
     * 启动时幂等删除（course_message 表已由 schema.sql 建出）。
     */
    private void dropLegacyNotificationTable() {
        List<String> tables = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                String.class, "notification");
        if (tables.isEmpty()) {
            return;
        }
        try {
            jdbc.execute("DROP TABLE `notification`");
            log.info("迁移完成：已删除遗留 notification 表（已由 course_message 取代）");
        } catch (Exception e) {
            log.warn("删除遗留 notification 表失败：{}", e.getMessage());
        }
    }

    /** 操作日志 + 自动备份记录表（CREATE TABLE IF NOT EXISTS，幂等） */
    private void createAuditTables() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS operation_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "user_id BIGINT COMMENT '操作人ID'," +
                "username VARCHAR(50) COMMENT '操作人用户名'," +
                "module VARCHAR(50) NOT NULL COMMENT '模块'," +
                "action VARCHAR(100) NOT NULL COMMENT '动作'," +
                "target_id BIGINT COMMENT '目标对象ID'," +
                "detail VARCHAR(1000) COMMENT '详情描述'," +
                "ip VARCHAR(50) COMMENT '来源IP'," +
                "success TINYINT(1) DEFAULT 1 COMMENT '是否成功'," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_oplog_time (created_at)," +
                "INDEX idx_oplog_user (user_id)," +
                "INDEX idx_oplog_module (module, action)" +
                ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
        jdbc.execute("CREATE TABLE IF NOT EXISTS backup_record (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "file_name VARCHAR(255) NOT NULL COMMENT '备份文件名'," +
                "file_path VARCHAR(500) COMMENT '相对路径或 COS key（失败时为空）'," +
                "file_size BIGINT COMMENT '字节数'," +
                "status VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '状态'," +
                "error_msg VARCHAR(500) COMMENT '失败原因'," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "INDEX idx_backup_time (created_at)" +
                ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
        // 存量表 file_path 原为 NOT NULL，失败备份无路径无法写入，放宽为可空（幂等）
        relaxColumnNotNull("backup_record", "file_path",
                "ALTER TABLE backup_record MODIFY COLUMN file_path VARCHAR(500) NULL COMMENT '相对路径或 COS key（失败时为空）'");
        addColumnIfMissing("backup_record", "storage_type",
                "ALTER TABLE backup_record ADD COLUMN storage_type VARCHAR(20) NOT NULL DEFAULT 'local' AFTER status");
        // 触发方式：存量记录无法区分来源，保持为空（不参与「当天是否已自动备份」判重，避免漏备）
        addColumnIfMissing("backup_record", "trigger_type",
                "ALTER TABLE backup_record ADD COLUMN trigger_type VARCHAR(20) NULL COMMENT '触发方式：auto（每日定时）/ manual（管理端手动），历史数据为空' AFTER storage_type");
    }

    /**
     * 课件模型重构：从「用户自定义分组 + biz_type/biz_id」收敛为「直接挂到学生」。
     * 旧表若仍有 biz_type 列（旧结构，biz_type NOT NULL 会阻断新 insert），直接 DROP 重建为新结构。
     * 同时丢弃旧 attachment_group 表（用户已确认附件数据全部清空）。
     * 幂等：新库由 schema.sql 直接建出新结构，本方法对已是新结构的库无副作用。
     */
    private void migrateAttachmentToStudent() {
        // 检测旧结构：attachment 表存在且含 biz_type 列 → 旧表，需重建
        List<String> bizCol = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attachment' AND COLUMN_NAME = 'biz_type'",
                String.class);
        if (!bizCol.isEmpty()) {
            try {
                jdbc.execute("DROP TABLE IF EXISTS `attachment`");
                jdbc.execute("CREATE TABLE attachment (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                        "user_id BIGINT NOT NULL," +
                        "student_id BIGINT COMMENT '所属学生 id'," +
                        "file_name VARCHAR(255) NOT NULL COMMENT '原始文件名'," +
                        "file_path VARCHAR(255) NOT NULL COMMENT '相对路径 /uploads/attachment/...'," +
                        "file_size BIGINT COMMENT '字节数'," +
                        "mime_type VARCHAR(100) COMMENT 'MIME 类型'," +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "INDEX idx_attachment_student (user_id, student_id)" +
                        ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
                log.info("迁移完成：attachment 表已重建为新结构（挂学生，旧 biz_type/group 列丢弃）");
            } catch (Exception e) {
                log.warn("重建 attachment 表失败：{}", e.getMessage());
            }
        } else {
            // 新结构或表不存在：若表存在但缺 student_id 列（极少见），补上
            addColumnIfMissing("attachment", "student_id",
                    "ALTER TABLE attachment ADD COLUMN student_id BIGINT NULL COMMENT '所属学生 id'");
        }
        // 删除遗留的 attachment_group 表（用户自定义分组体系已移除）
        List<String> groups = jdbc.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                String.class, "attachment_group");
        if (!groups.isEmpty()) {
            try {
                jdbc.execute("DROP TABLE `attachment_group`");
                log.info("迁移完成：已删除遗留 attachment_group 表（课件改为挂载到学生）");
            } catch (Exception e) {
                log.warn("删除遗留 attachment_group 表失败：{}", e.getMessage());
            }
        }
    }

    /**
     * 课件改为一对多：attachment.student_id 单列拆出为 attachment_student 关联表，
     * 一个课件可关联多个学生。
     * 幂等：schema.sql 已建关联表；存量 attachment.student_id 数据搬入关联表后
     * 把旧列清为 NULL 并保留列结构（便于多次启动时通过「列非空」判断是否已迁移）。
     */
    private void migrateAttachmentMultiStudent() {
        // 1. 关联表：schema.sql 的 CREATE TABLE IF NOT EXISTS 已保证存在，此处兜底
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS attachment_student (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id BIGINT NOT NULL COMMENT '所属用户'," +
                    "attachment_id BIGINT NOT NULL COMMENT '课件 id'," +
                    "student_id BIGINT NOT NULL COMMENT '学生 id'," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE KEY uk_attachment_student (attachment_id, student_id)," +
                    "INDEX idx_attachment_user_student (user_id, student_id)" +
                    ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4");
        } catch (Exception e) {
            log.warn("创建 attachment_student 关联表失败：{}", e.getMessage());
            return;
        }
        // 2. 存量数据搬运：attachment.student_id 非空的行 → 关联表
        //    （IGNORE 避免重复启动时唯一键冲突；迁移后清空旧列，保证下次启动无行可搬）
        try {
            int moved = jdbc.update("INSERT IGNORE INTO attachment_student(user_id, attachment_id, student_id) " +
                    "SELECT a.user_id, a.id, a.student_id FROM attachment a " +
                    "WHERE a.student_id IS NOT NULL");
            if (moved > 0) {
                log.info("迁移完成：attachment.student_id 已拆出为多学生关联表，共搬运 {} 行", moved);
                jdbc.update("UPDATE attachment SET student_id = NULL WHERE student_id IS NOT NULL");
            }
        } catch (Exception e) {
            log.warn("attachment.student_id 数据搬入关联表失败：{}", e.getMessage());
        }
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
        String[][] tables = {{"course", "course_type"}, {"student", "course_type"}};
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

    /**
     * 把指定列从 NOT NULL 放宽为可空（存量表兼容，幂等）。
     * 用于无法用 CREATE TABLE IF NOT EXISTS 修正约束的情况（如失败记录无 file_path）。
     */
    private void relaxColumnNotNull(String table, String column, String ddl) {
        try {
            jdbc.execute(ddl);
            log.info("迁移完成：{}.{} 已放宽为可空", table, column);
        } catch (Exception e) {
            // 列已是可空 / 表不存在 / 其他：静默跳过，不影响启动
            log.debug("放宽 {}.{} 可空跳过：{}", table, column, e.getMessage());
        }
    }
}
