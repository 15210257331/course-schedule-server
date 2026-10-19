package com.chenxiaofei.coursescheduleserver.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教师详情（管理端，含本月 / 上月课程与收入统计）
 */
@Data
public class TeacherDetail {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    /** 任教学科，逗号分隔 */
    private String subjects;
    /** 账号状态：active/disabled */
    private String status;
    private String disabledReason;
    private LocalDateTime disabledAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    /** 本月总课数 */
    private Long monthTotal;
    /** 本月已上课数（已结束） */
    private Long monthFinished;
    /** 本月已有收入（已结束课程课时费合计） */
    private BigDecimal monthIncome;
    /** 上月总课数 */
    private Long lastMonthTotal;
    /** 上月收入 */
    private BigDecimal lastMonthIncome;
}
