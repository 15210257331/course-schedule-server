package com.chenxiaofei.coursescheduleserver.auth.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginResponse;
import com.chenxiaofei.coursescheduleserver.auth.dto.PasswordUpdateRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.RegisterRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.ResetCodeRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.ResetPasswordRequest;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.mail.MailService;
import com.chenxiaofei.coursescheduleserver.security.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final CaptchaStore captchaStore;
    private final MailService mailService;

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, CaptchaStore captchaStore, MailService mailService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.captchaStore = captchaStore;
        this.mailService = mailService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null || !BCrypt.checkpw(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        if ("disabled".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        userMapper.updateLastLoginAt(user.getId());
        return buildResponse(user);
    }

    public LoginResponse register(RegisterRequest request) {
        User existing = userMapper.findByUsername(request.getUsername());
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        if (userMapper.findByEmail(request.getEmail().trim()) != null) {
            throw new BusinessException("该邮箱已被注册");
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
        user.setSubjects(request.getSubjects());
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

    /** 发送密码重置验证码（按邮箱定位用户） */
    public void sendResetCode(ResetCodeRequest request) {
        String email = request.getEmail().trim();
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new BusinessException(404, "该邮箱未注册");
        }
        String code = captchaStore.issue(email);
        mailService.sendResetCode(email, code);
    }

    /** 校验验证码并重置密码 */
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().trim();
        captchaStore.verify(email, request.getCode());
        User user = userMapper.findByEmail(email);
        if (user == null) {
            throw new BusinessException(404, "该邮箱未注册");
        }
        userMapper.updatePassword(user.getId(), BCrypt.hashpw(request.getNewPassword(), BCrypt.gensalt()));
    }

    private LoginResponse buildResponse(User user) {
        LoginResponse resp = new LoginResponse();
        resp.setToken(jwtUtil.generate(user.getId(), user.getRole(), user.getNickname()));
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setRole(user.getRole());
        resp.setSubjects(user.getSubjects());
        resp.setEmail(user.getEmail());
        return resp;
    }
}