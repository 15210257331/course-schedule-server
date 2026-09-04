package com.chenxiaofei.coursescheduleserver.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师列表项（管理端，带统计信息）
 */
@Data
public class TeacherListItem {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String subjects;
    private String status;
    private String disabledReason;
    private LocalDateTime disabledAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;

    /** 课程总数 */
    private Long courseCount;
    /** 学生数（去重） */
    private Long studentCount;
    /** 机构数 */
    private Long organizationCount;
}
