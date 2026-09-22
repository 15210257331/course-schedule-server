package com.chenxiaofei.coursescheduleserver.auth.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.IpUtil;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginResponse;
import com.chenxiaofei.coursescheduleserver.auth.dto.PasswordUpdateRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.RegisterRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.ResetCodeRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.ResetPasswordRequest;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.mail.MailService;
import com.chenxiaofei.coursescheduleserver.operationlog.entity.OperationLog;
import com.chenxiaofei.coursescheduleserver.operationlog.service.OperationLogService;
import com.chenxiaofei.coursescheduleserver.security.JwtUtil;
import com.chenxiaofei.coursescheduleserver.security.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final CaptchaStore captchaStore;
    private final MailService mailService;
    private final LoginAttemptGuard loginAttemptGuard;
    private final OperationLogService operationLogService;
    private final PasswordService passwordService;

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername() == null ? null : request.getUsername().trim();
        String ip = IpUtil.resolveIp();

        // 防暴力破解：锁定期间直接拒绝
        loginAttemptGuard.check(username, ip);

        User user = userMapper.findByUsername(username);
        if (user == null || !passwordService.matches(request.getPassword(), user.getPassword())) {
            loginAttemptGuard.onFailure(username, ip);
            recordLogin(username, "LOGIN_FAIL", false, "用户名或密码错误", ip);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if ("disabled".equals(user.getStatus())) {
            loginAttemptGuard.onFailure(username, ip);
            recordLogin(username, "LOGIN_FAIL", false, "账号已被禁用", ip);
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        loginAttemptGuard.onSuccess(username, ip);
        userMapper.updateLastLoginAt(user.getId());
        recordLogin(username, "LOGIN", true, null, ip);
        return buildResponse(user);
    }

    /** 记录登录日志（登录前无 UserContext，需手动写入用户名） */
    private void recordLogin(String username, String action, boolean success, String failMsg, String ip) {
        try {
            OperationLog log = new OperationLog();
            log.setUsername(username);
            log.setModule("auth");
            log.setAction(action);
            log.setDetail(failMsg);
            log.setIp(ip);
            log.setSuccess(success);
            operationLogService.record(log);
        } catch (Exception e) {
            // 登录日志写入失败不影响登录流程
        }
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
        user.setPassword(passwordService.encode(request.getPassword()));
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
        if (user == null || !passwordService.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(userId, passwordService.encode(request.getNewPassword()));
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
        userMapper.updatePassword(user.getId(), passwordService.encode(request.getNewPassword()));
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