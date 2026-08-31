package com.chenxiaofei.coursescheduleserver.coursetemplate.dto;

import lombok.Data;

/**
 * 课程模板（学生）分页查询参数（POST body）
 */
@Data
public class CourseTemplatePageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 20;

    /** 学生姓名模糊查询 */
    private String name;
}