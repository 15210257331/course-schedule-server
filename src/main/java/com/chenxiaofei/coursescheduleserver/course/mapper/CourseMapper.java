package com.chenxiaofei.coursescheduleserver.course.mapper;

import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseMapper {

    List<Course> listInRange(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end, @Param("title") String title);

    long countInRange(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end, @Param("title") String title);

    List<Course> pageInRange(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end, @Param("title") String title,
                             @Param("offset") long offset, @Param("limit") int limit);

    Course findById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Course c);

    int update(Course c);

    int updateTime(@Param("id") Long id, @Param("userId") Long userId,
                   @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    /** 按模板删除其排出的全部课程（删除模板时级联删除课程） */
    int deleteByTemplate(@Param("userId") Long userId, @Param("templateId") Long templateId);

    /** 按模板列出其排出的全部课程（编辑模板时同步课程字段用） */
    List<Course> listByTemplate(@Param("userId") Long userId, @Param("templateId") Long templateId);

    /** 解除课程与模板的关联（模板删除但课程保留时） */
    int clearTemplate(@Param("userId") Long userId, @Param("templateId") Long templateId);

    int countConflict(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    int countConflictExclude(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId);

    int countByParent(@Param("userId") Long userId, @Param("parentId") Long parentId);

    /** 按机构统计课程数（删除保护用） */
    long countByOrganization(@Param("userId") Long userId, @Param("organizationId") Long organizationId);

    /** 到期未结的 scheduled 课程（end_time < now），用于按时间自动结算为 completed */
    List<Course> listExpiredScheduled(@Param("userIds") List<Long> userIds, @Param("now") LocalDateTime now);

    /** 到点未提醒的 scheduled 课程（now 已到达 start_time - 提醒偏移），用于生成提醒 */
    List<Course> listDueReminder(@Param("userIds") List<Long> userIds, @Param("now") LocalDateTime now);

    /** 批量更新课程状态 */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 按 user_id 加 MySQL 会话级 advisory lock，串行化同一教师的课程写操作，
     * 消除「先 countConflict 再 insert」的 TOCTOU 并发竞态。
     * 返回 1=加锁成功，0=超时，NULL=出错。
     */
    @Select("SELECT GET_LOCK(#{name}, #{timeout})")
    Integer getLock(@Param("name") String name, @Param("timeout") int timeout);

    /** 释放 {@link #getLock} 获取的锁，返回 1=释放成功 */
    @Select("SELECT RELEASE_LOCK(#{name})")
    Integer releaseLock(@Param("name") String name);
}
