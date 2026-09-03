package com.chenxiaofei.coursescheduleserver.dashboard.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机构/家教明细行的月结状态。
 * 粒度 = 用户 × 月份 × 行（类型 + 行键），一行一条「是否已结清」记录。
 */
@Data
public class Settlement {

    private Long id;
    private Long userId;
    /** 结算月份，格式 YYYY-MM */
    private String settleMonth;
    /** 行类型：org（机构/未分类） / tutor（家教-学生） */
    private String targetType;
    /** 行键：机构名 / 学生名（未分类为「未分类」） */
    private String targetKey;
    private Boolean settled;
    private LocalDateTime updatedAt;
}