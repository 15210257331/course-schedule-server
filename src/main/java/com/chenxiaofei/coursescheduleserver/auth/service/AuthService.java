package com.chenxiaofei.coursescheduleserver.auth.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginResponse;
import com.chenxiaofei.coursescheduleserver.auth.dto.PasswordUpdateRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.RegisterRequest;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.security.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return buildResponse(user);
    }

    public LoginResponse register(RegisterRequest request) {
        User existing = userMapper.findByUsername(request.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(BCrypt.hashpw(request.getPassword(), BCrypt.gensalt()));
        user.setNickname(request.getNickname() == null || request.getNickname().isBlank()
                ? request.getUsername() : request.getNickname());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole("TEACHER");
        userMapper.insert(user);
        return buildResponse(user);
    }

    public User profile(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public User updateProfile(Long userId, User request) {
        User user = new User();
        user.setId(userId);
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        userMapper.updateProfile(user);
        return profile(userId);
    }

    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userMapper.findById(userId);
        if (user == null || !BCrypt.checkpw(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(userId, BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
    }

    private LoginResponse buildResponse(User user) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.generate(user.getId(), user.getRole(), user.getNickname()));
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setRole(user.getRole());
        return resp;
    }
}