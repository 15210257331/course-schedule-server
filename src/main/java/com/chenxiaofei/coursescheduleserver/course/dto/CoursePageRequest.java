package com.chenxiaofei.coursescheduleserver.course.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 课程分页查询参数（POST body）
 */
@Data
public class CoursePageRequest {

    private Integer pageNum = 1;
    private Integer pageSize = 20;

    /** 标题模糊查询 */
    private String title;

    private LocalDateTime start;
    private LocalDateTime end;
}