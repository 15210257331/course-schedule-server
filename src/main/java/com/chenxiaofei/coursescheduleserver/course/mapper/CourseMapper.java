package com.chenxiaofei.coursescheduleserver.course.mapper;

import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    int deleteAllByUser(@Param("userId") Long userId);

    int countConflict(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    int countConflictExclude(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId);

    int countByParent(@Param("userId") Long userId, @Param("parentId") Long parentId);

    /** 按机构统计课程数（删除保护用） */
    long countByOrganization(@Param("userId") Long userId, @Param("organizationId") Long organizationId);

    /** 该用户全部课程（数据备份导出用，按时间升序） */
    List<Course> listAllByUser(@Param("userId") Long userId);

    /** 备份导入：回填重复系列父课程 */
    int updateParent(@Param("id") Long id, @Param("userId") Long userId, @Param("parentId") Long parentId);

    /** 到期未结的 scheduled 课程（end_time < now），用于按时间自动结算为 completed */
    List<Course> listExpiredScheduled(@Param("userIds") List<Long> userIds, @Param("now") LocalDateTime now);

    /** 到点未提醒的 scheduled 课程（now 已到达 start_time - 提醒偏移），用于生成提醒 */
    List<Course> listDueReminder(@Param("userIds") List<Long> userIds, @Param("now") LocalDateTime now);

    /** 批量更新课程状态 */
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);
}
