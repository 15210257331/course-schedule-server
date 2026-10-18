package com.chenxiaofei.coursescheduleserver.student.mapper;

import com.chenxiaofei.coursescheduleserver.student.dto.StudentPageRequest;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {

    List<Student> listByUser(@Param("userId") Long userId, @Param("name") String name);

    long countByUser(@Param("userId") Long userId, @Param("req") StudentPageRequest req);

    List<Student> pageByUser(@Param("userId") Long userId, @Param("req") StudentPageRequest req,
                                    @Param("offset") long offset, @Param("limit") int limit);

    Student findById(@Param("id") Long id, @Param("userId") Long userId);

    int countByStudentName(@Param("userId") Long userId, @Param("studentName") String studentName, @Param("excludeId") Long excludeId);

    int insert(Student t);

    int update(Student t);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
