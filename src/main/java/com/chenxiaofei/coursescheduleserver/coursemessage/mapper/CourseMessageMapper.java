package com.chenxiaofei.coursescheduleserver.coursemessage.mapper;

import com.chenxiaofei.coursescheduleserver.coursemessage.entity.CourseMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseMessageMapper {

    List<CourseMessage> listDue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<CourseMessage> listRecent(@Param("userId") Long userId, @Param("limit") int limit);

    int insert(CourseMessage n);

    /** 某课程是否已生成过提醒（去重用） */
    int countByCourse(@Param("userId") Long userId, @Param("courseId") Long courseId);

    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
