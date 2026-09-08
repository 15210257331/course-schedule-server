-- ============================================================
-- TeacherOS 数据库重置脚本（清空 course_schedule 库全部表）
-- ------------------------------------------------------------
-- 用途：清掉现有数据，随后按 schema.sql 重新建表
-- 用法：
--   1) 停掉后端服务
--   2) 执行本脚本：
--        mysql -h <host> -P <port> -u root -p course_schedule < reset.sql
--   3) 重启后端服务，schema.sql（spring.sql.init）会自动重建全部表，
--      并种入默认账号 admin / admin123
-- 注意：会删除全部业务数据，执行前请先备份：
--        mysqldump -h <host> -P <port> -u root -p course_schedule > backup.sql
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `attachment`;
DROP TABLE IF EXISTS `attachment_group`;
DROP TABLE IF EXISTS `admin_message_read`;
DROP TABLE IF EXISTS `admin_message`;
DROP TABLE IF EXISTS `settlement`;
DROP TABLE IF EXISTS `setting`;
DROP TABLE IF EXISTS `notification`;
DROP TABLE IF EXISTS `course_template`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `organization`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- ------------------------------------------------------------
-- 备选：彻底删库重建（需在停服状态下，且用 root 直连，不指定库）
--   DROP DATABASE IF EXISTS course_schedule;
--   CREATE DATABASE course_schedule DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
-- 之后重启服务，schema.sql 会自动建表。
-- ------------------------------------------------------------