package com.chenxiaofei.coursescheduleserver.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private String subjects;
    private String email;
}