package com.chenxiaofei.coursescheduleserver.coursetemplate.mapper;

import com.chenxiaofei.coursescheduleserver.coursetemplate.entity.CourseTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseTemplateMapper {

    List<CourseTemplate> listByUser(@Param("userId") Long userId, @Param("name") String name);

    long countByUser(@Param("userId") Long userId, @Param("name") String name);

    List<CourseTemplate> pageByUser(@Param("userId") Long userId, @Param("name") String name,
                                    @Param("offset") long offset, @Param("limit") int limit);

    List<CourseTemplate> listAllByUser(@Param("userId") Long userId);

    CourseTemplate findById(@Param("id") Long id, @Param("userId") Long userId);

    int countByStudentName(@Param("userId") Long userId, @Param("studentName") String studentName, @Param("excludeId") Long excludeId);

    int insert(CourseTemplate t);

    int update(CourseTemplate t);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    int deleteAllByUser(@Param("userId") Long userId);
}
