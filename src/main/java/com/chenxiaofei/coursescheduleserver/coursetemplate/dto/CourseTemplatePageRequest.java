package com.chenxiaofei.coursescheduleserver.coursetemplate.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程模板（学生）分页查询参数（POST body）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CourseTemplatePageRequest extends PageRequest {

    /** 学生姓名模糊查询 */
    private String name;
}