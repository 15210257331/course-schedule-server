package com.chenxiaofei.coursescheduleserver.course.mapper;

import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseMapper {

    List<Course> listInRange(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    Course findById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Course c);

    int update(Course c);

    int updateTime(@Param("id") Long id, @Param("userId") Long userId,
                   @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    int countConflict(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                      @Param("end") LocalDateTime end);

    int countConflictExclude(@Param("userId") Long userId, @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end, @Param("excludeId") Long excludeId);

    int countByParent(@Param("userId") Long userId, @Param("parentId") Long parentId);
}
