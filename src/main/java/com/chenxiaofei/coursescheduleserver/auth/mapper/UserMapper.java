package com.chenxiaofei.coursescheduleserver.auth.mapper;

import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    int insert(User user);

    int updateProfile(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
