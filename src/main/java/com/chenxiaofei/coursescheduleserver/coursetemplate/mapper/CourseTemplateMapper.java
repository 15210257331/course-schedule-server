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

    CourseTemplate findById(@Param("id") Long id, @Param("userId") Long userId);

    int countByStudent(@Param("userId") Long userId, @Param("studentId") Long studentId, @Param("excludeId") Long excludeId);

    int insert(CourseTemplate t);

    int update(CourseTemplate t);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
