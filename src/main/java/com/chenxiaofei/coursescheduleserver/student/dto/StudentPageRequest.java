package com.chenxiaofei.coursescheduleserver.student.dto;

import com.chenxiaofei.coursescheduleserver.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学生分页查询参数（POST body）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StudentPageRequest extends PageRequest {

    /** 学生姓名模糊查询 */
    private String name;

    /** 学段筛选：初一/初二/初三/高一/高二/高三 */
    private String stage;

    /** 课程类型筛选：一对一/家教/班课 */
    private String courseType;

    /** 机构筛选 */
    private Long organizationId;
}
