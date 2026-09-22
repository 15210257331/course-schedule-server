package com.chenxiaofei.coursescheduleserver.auth.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginResponse;
import com.chenxiaofei.coursescheduleserver.auth.dto.PasswordUpdateRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.RegisterRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.ResetCodeRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.ResetPasswordRequest;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @PostMapping("/profile")
    public Result<User> profile() {
        return Result.ok(authService.profile(UserContext.getUserId()));
    }

    @PutMapping("/profile")
    public Result<User> updateProfile(@RequestBody User request) {
        return Result.ok(authService.updateProfile(UserContext.getUserId(), request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        authService.updatePassword(UserContext.getUserId(), request);
        return Result.ok();
    }

    @PostMapping("/reset-code")
    public Result<Void> sendResetCode(@Valid @RequestBody ResetCodeRequest request) {
        authService.sendResetCode(request);
        return Result.ok();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return Result.ok();
    }
}