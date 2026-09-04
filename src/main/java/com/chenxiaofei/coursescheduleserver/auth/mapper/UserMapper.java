package com.chenxiaofei.coursescheduleserver.auth.mapper;

import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    User findByEmail(@Param("email") String email);

    List<Long> listAllIds();

    int insert(User user);

    int updateProfile(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int updateLastLoginAt(@Param("id") Long id);

    // ========== 管理端 ==========

    long countTeachers(@Param("status") String status, @Param("keyword") String keyword);

    List<User> pageTeachers(@Param("status") String status, @Param("keyword") String keyword,
                            @Param("offset") long offset, @Param("limit") int limit);

    /** 教师列表：带课程数统计 */
    List<com.chenxiaofei.coursescheduleserver.admin.dto.TeacherListItem> pageTeacherItems(
            @Param("status") String status, @Param("keyword") String keyword,
            @Param("offset") long offset, @Param("limit") int limit);

    int updateStatus(@Param("id") Long id, @Param("status") String status,
                     @Param("disabledReason") String disabledReason);

    /** 全部教师简要列表（消息推送选择器用） */
    List<User> listTeachers(@Param("keyword") String keyword);
}
