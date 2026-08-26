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
    role        VARCHAR(20)  NOT NULL DEFAULT 'TEACHER',
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
    default_fee   DECIMAL(10, 2),
    remark        VARCHAR(500),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_org_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS student (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT      NOT NULL,
    name            VARCHAR(50) NOT NULL,
    gender          VARCHAR(10),
    grade           VARCHAR(20),
    subject         VARCHAR(50),
    organization_id BIGINT,
    phone           VARCHAR(20),
    parent_phone    VARCHAR(20),
    fee             DECIMAL(10, 2),
    remark          VARCHAR(500),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_stu_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS course (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                  BIGINT       NOT NULL,
    title                    VARCHAR(100) NOT NULL,
    student_id               BIGINT,
    organization_id          BIGINT,
    subject                  VARCHAR(50),
    course_type              VARCHAR(50),
    start_time               DATETIME     NOT NULL,
    end_time                 DATETIME     NOT NULL,
    fee                      DECIMAL(10, 2),
    fee_manual               TINYINT DEFAULT 0,
    location                 VARCHAR(200),
    note                     VARCHAR(800),
    status                   VARCHAR(20) DEFAULT 'scheduled',
    color                    VARCHAR(20),
    reminder_offset_minutes  INT DEFAULT 30,
    repeat_type              VARCHAR(20),
    repeat_end_date          DATE,
    parent_id                BIGINT,
    created_at               DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_course_user (user_id),
    INDEX idx_course_start (user_id, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS salary_rule (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    organization_id BIGINT,
    grade           VARCHAR(20),
    subject         VARCHAR(50),
    hourly_fee      DECIMAL(10, 2) NOT NULL,
    remark          VARCHAR(200),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rule_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS notification (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    type       VARCHAR(20),
    title      VARCHAR(100),
    content    VARCHAR(500),
    course_id  BIGINT,
    remind_at  DATETIME,
    is_read    TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notify_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS setting (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    setting_key   VARCHAR(50) NOT NULL,
    setting_value VARCHAR(500),
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_setting (user_id, setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;