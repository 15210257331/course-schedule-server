package com.chenxiaofei.coursescheduleserver.auth.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.LoginResponse;
import com.chenxiaofei.coursescheduleserver.auth.dto.PasswordUpdateRequest;
import com.chenxiaofei.coursescheduleserver.auth.dto.RegisterRequest;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @GetMapping("/profile")
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
}