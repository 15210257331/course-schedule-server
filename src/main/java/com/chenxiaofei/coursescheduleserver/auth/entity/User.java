package com.chenxiaofei.coursescheduleserver.auth.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}