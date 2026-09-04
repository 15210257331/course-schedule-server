package com.chenxiaofei.coursescheduleserver.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板统计
 */
@Mapper
public interface AdminStatsMapper {

    /** 教师总数（可含已禁用） */
    long countTeachers();

    /** 新增教师数（start 起，含当日） */
    long countNewTeachers(@Param("start") LocalDate start);

    /** 课程总数（全体教师） */
    long countCourses();

    /** 时间段内课程数 */
    long countCoursesInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 时间段内新增教师数（按天） */
    List<Map<String, Object>> teacherTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 时间段内课程数（按天） */
    List<Map<String, Object>> courseTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** 已发布消息总数 */
    long countPublishedMessages();

    /** 消息阅读总记录数 */
    long countMessageReads();

    /** 最近发布的消息（前 N 条，含阅读数） */
    List<com.chenxiaofei.coursescheduleserver.admin.entity.AdminMessage> recentMessages(@Param("limit") int limit);

    /** 最近注册的教师（前 N 条） */
    List<com.chenxiaofei.coursescheduleserver.auth.entity.User> recentTeachers(@Param("limit") int limit);
}
