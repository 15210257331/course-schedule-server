package com.chenxiaofei.coursescheduleserver.student.mapper;

import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper {

    List<Student> listByUser(@Param("userId") Long userId);

    Student findById(@Param("id") Long id, @Param("userId") Long userId);

    Student findByName(@Param("userId") Long userId, @Param("name") String name);

    int insert(Student s);

    int update(Student s);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
