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

    /** 学生姓名模糊查询 */
    private String studentName;

    /** 科目 */
    private String subject;

    /** 学段 */
    private String stage;

    /** 课程类型 */
    private String courseType;

    /** 上课机构 id */
    private Long organizationId;

    /** 课程状态 */
    private String status;
}