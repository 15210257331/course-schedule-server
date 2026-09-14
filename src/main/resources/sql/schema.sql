-- TeacherOS 数据库表结构（幂等，可重复执行）
-- 数据库：course_schedule

CREATE TABLE IF NOT EXISTS `user` (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    avatar      VARCHAR(255),
    email       VARCHAR(100),
    phone       VARCHAR(20),
    subjects    VARCHAR(200),
    role        VARCHAR(20)  NOT NULL DEFAULT 'TEACHER' COMMENT '角色：TEACHER/ADMIN',
    status      VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT '账号状态：active/disabled',
    disabled_reason VARCHAR(500),
    disabled_at DATETIME,
    last_login_at DATETIME,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS organization (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    contact_name  VARCHAR(50),
    contact_phone VARCHAR(20),
    address       VARCHAR(255),
    color         VARCHAR(20),
    remark        VARCHAR(500),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_org_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS course (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                  BIGINT       NOT NULL,
    title                    VARCHAR(100) NOT NULL,
    student_name             VARCHAR(50),
    organization_id          BIGINT,
    subject                  VARCHAR(50),
    stage                    VARCHAR(20),
    course_type              VARCHAR(50),
    start_time               DATETIME     NOT NULL,
    end_time                 DATETIME     NOT NULL,
    fee                      DECIMAL(10, 2),
    location                 VARCHAR(200),
    note                     VARCHAR(800),
    status                   VARCHAR(20) DEFAULT 'scheduled',
    color                    VARCHAR(20),
    reminder_offset_minutes  INT DEFAULT 30,
    repeat_type              VARCHAR(20),
    repeat_end_date          DATE,
    parent_id                BIGINT,
    template_id              BIGINT,
    created_at               DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_user (user_id),
    INDEX idx_course_start (user_id, start_time),
    INDEX idx_course_tpl (user_id, template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS course_template (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT       NOT NULL,
    title             VARCHAR(100) NOT NULL,
    student_name      VARCHAR(50),
    organization_id   BIGINT,
    subject           VARCHAR(50),
    stage             VARCHAR(20),
    course_type       VARCHAR(50),
    duration_minutes  INT          NOT NULL DEFAULT 60,
    fee               DECIMAL(10, 2),
    location          VARCHAR(200),
    note              VARCHAR(800),
    repeat_type       VARCHAR(20) DEFAULT NULL COMMENT '拖入日历时的重复规则：daily/weekly/biweekly，NULL 不重复',
    created_at        DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tpl_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS course_message (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    title      VARCHAR(100),
    content    VARCHAR(500),
    course_id  BIGINT,
    remind_at  DATETIME,
    is_read    TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cmsg_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS setting (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    setting_key   VARCHAR(50) NOT NULL,
    setting_value VARCHAR(500),
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_setting (user_id, setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS settlement (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    settle_month VARCHAR(7)   NOT NULL COMMENT '结算月份 YYYY-MM',
    target_type  VARCHAR(20)  NOT NULL COMMENT '行类型：org（机构/未分类）/ tutor（家教-学生）',
    target_key   VARCHAR(100) NOT NULL COMMENT '行键：机构名 / 学生名',
    settled      TINYINT(1)   DEFAULT 0 COMMENT '是否已结清',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_settlement (user_id, settle_month, target_type, target_key),
    INDEX idx_settlement_month (user_id, settle_month)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ========== 管理端 ==========

-- 管理员消息表
CREATE TABLE IF NOT EXISTS admin_message (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL COMMENT '消息标题',
    content      TEXT         NOT NULL COMMENT '消息内容',
    type         VARCHAR(20)  NOT NULL DEFAULT 'announcement' COMMENT '类型：announcement/activity/notice',
    target_type  VARCHAR(20)  NOT NULL DEFAULT 'all' COMMENT '目标：all/specific',
    target_ids   VARCHAR(2000) COMMENT '目标教师ID列表，逗号分隔（target_type=specific 时）',
    status       VARCHAR(20)  NOT NULL DEFAULT 'published' COMMENT '状态：published/revoked',
    created_by   BIGINT       NOT NULL COMMENT '发布管理员ID',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_msg_status (status),
    INDEX idx_msg_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 消息阅读记录表
CREATE TABLE IF NOT EXISTS admin_message_read (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    read_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_user (message_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ========== 附件 ==========

-- 操作日志：记录关键写操作（登录 / 管理端操作等），供管理端审计
CREATE TABLE IF NOT EXISTS operation_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT COMMENT '操作人ID（登录失败等场景可为空）',
    username     VARCHAR(50) COMMENT '操作人用户名',
    module       VARCHAR(50)  NOT NULL COMMENT '模块：auth/course/teacher/message/backup...',
    action       VARCHAR(100) NOT NULL COMMENT '动作：LOGIN/LOGIN_FAIL/CREATE/UPDATE/DELETE/STATUS...',
    target_id    BIGINT COMMENT '目标对象ID（可为空）',
    detail       VARCHAR(1000) COMMENT '详情描述',
    ip           VARCHAR(50) COMMENT '来源IP',
    success      TINYINT(1) DEFAULT 1 COMMENT '是否成功',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_oplog_time (created_at),
    INDEX idx_oplog_user (user_id),
    INDEX idx_oplog_module (module, action)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 自动备份：定时任务生成的备份文件记录
CREATE TABLE IF NOT EXISTS backup_record (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name    VARCHAR(255) NOT NULL COMMENT '备份文件名',
    file_path    VARCHAR(500) COMMENT '相对路径或 COS key（失败时为空）',
    file_size    BIGINT COMMENT '字节数',
    status       VARCHAR(20) NOT NULL DEFAULT 'success' COMMENT '状态：success/failed',
    storage_type VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT '存储位置：local/cos',
    error_msg    VARCHAR(500) COMMENT '失败原因',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_backup_time (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 附件：挂在课程模板（学生）下的资源文件元数据
CREATE TABLE IF NOT EXISTS attachment (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    template_id  BIGINT COMMENT '所属课程模板 id',
    file_name    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path    VARCHAR(255) NOT NULL COMMENT '相对路径 /uploads/attachment/...',
    file_size    BIGINT COMMENT '字节数',
    mime_type    VARCHAR(100) COMMENT 'MIME 类型',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_attachment_tpl (user_id, template_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;