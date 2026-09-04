package com.chenxiaofei.coursescheduleserver.admin.dto;

import lombok.Data;

/**
 * 教师分页查询参数（管理端）
 */
@Data
public class TeacherPageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 20;

    /** 状态筛选：active/disabled，空为全部 */
    private String status;

    /** 关键词：用户名/昵称/邮箱模糊 */
    private String keyword;
}
