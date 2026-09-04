package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherListItem;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherPageRequest;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherStatusRequest;
import com.chenxiaofei.coursescheduleserver.admin.service.AdminTeacherService;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.security.AdminGuard;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端 - 教师（用户）管理
 */
@RestController
@RequestMapping("/api/admin/teachers")
public class AdminTeacherController {

    private final AdminTeacherService service;
    private final AdminGuard adminGuard;

    public AdminTeacherController(AdminTeacherService service, AdminGuard adminGuard) {
        this.service = service;
        this.adminGuard = adminGuard;
    }

    @PostMapping("/page")
    public Result<PageResult<TeacherListItem>> page(@RequestBody TeacherPageRequest request) {
        adminGuard.requireAdmin();
        return Result.ok(service.page(request));
    }

    @PostMapping("/detail")
    public Result<User> detail(@RequestBody Map<String, Long> body) {
        adminGuard.requireAdmin();
        return Result.ok(service.detail(body.get("id")));
    }

    @PostMapping("/status")
    public Result<Void> updateStatus(@RequestBody TeacherStatusRequest request) {
        adminGuard.requireAdmin();
        service.updateStatus(request);
        return Result.ok();
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@RequestBody Map<String, Long> body) {
        adminGuard.requireAdmin();
        service.resetPassword(body.get("id"));
        return Result.ok();
    }
}
