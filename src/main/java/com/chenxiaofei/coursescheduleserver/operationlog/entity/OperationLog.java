package com.chenxiaofei.coursescheduleserver.operationlog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志：记录关键写操作（登录 / 管理端操作等），供管理端审计。
 */
@Data
public class OperationLog {

    private Long id;
    /** 操作人ID（登录失败等场景可为空） */
    private Long userId;
    /** 操作人用户名 */
    private String username;
    /** 模块：auth/course/teacher/message/backup... */
    private String module;
    /** 动作：LOGIN/LOGIN_FAIL/CREATE/UPDATE/DELETE/STATUS... */
    private String action;
    /** 目标对象ID（可为空） */
    private Long targetId;
    /** 详情描述 */
    private String detail;
    /** 来源IP */
    private String ip;
    /** 是否成功 */
    private Boolean success;
    private LocalDateTime createdAt;
}
