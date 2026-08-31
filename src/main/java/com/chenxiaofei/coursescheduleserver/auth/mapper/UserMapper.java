package com.chenxiaofei.coursescheduleserver.auth.mapper;

import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findById(@Param("id") Long id);

    User findByUsername(@Param("username") String username);

    List<Long> listAllIds();

    int insert(User user);

    int updateProfile(User user);

    int updatePassword(@Param("id") Long id, @Param("password") String password);
}
