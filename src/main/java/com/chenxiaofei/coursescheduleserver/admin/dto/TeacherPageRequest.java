package com.chenxiaofei.coursescheduleserver.admin.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教师分页查询参数（管理端）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TeacherPageRequest extends PageRequest {

    /** 状态筛选：active/disabled，空为全部 */
    private String status;

    /** 关键词：用户名/昵称/邮箱模糊 */
    private String keyword;
}
