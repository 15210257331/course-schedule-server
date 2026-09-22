package com.chenxiaofei.coursescheduleserver.operationlog.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志分页查询参数（管理端）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OperationLogPageRequest extends PageRequest {

    /** 模块筛选：auth/course/teacher/message/backup，空为全部 */
    private String module;

    /** 动作筛选：LOGIN/CREATE/UPDATE/DELETE...，空为全部 */
    private String action;

    /** 关键词：用户名 / 详情模糊 */
    private String keyword;
}
