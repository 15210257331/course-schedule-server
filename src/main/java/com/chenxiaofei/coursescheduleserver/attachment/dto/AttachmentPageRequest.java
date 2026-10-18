package com.chenxiaofei.coursescheduleserver.attachment.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课件分页查询参数（POST body）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AttachmentPageRequest extends PageRequest {

    /** 所属学生筛选，空为全部 */
    private Long studentId;

    /** 文件名关键字模糊查询 */
    private String keyword;
}
