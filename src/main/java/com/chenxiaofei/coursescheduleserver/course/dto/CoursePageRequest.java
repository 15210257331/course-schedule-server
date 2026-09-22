package com.chenxiaofei.coursescheduleserver.course.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 课程分页查询参数（POST body）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CoursePageRequest extends PageRequest {

    /** 标题模糊查询 */
    private String title;

    private LocalDateTime start;
    private LocalDateTime end;
}