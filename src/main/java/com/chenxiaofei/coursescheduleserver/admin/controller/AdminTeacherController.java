package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherListItem;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherPageRequest;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherStatusRequest;
import com.chenxiaofei.coursescheduleserver.admin.service.AdminTeacherService;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.common.IdRequest;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 教师（用户）管理
 */
@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final AdminTeacherService service;

    @PostMapping("/page")
    public Result<PageResult<TeacherListItem>> page(@RequestBody TeacherPageRequest request) {
        return Result.ok(service.page(request));
    }

    @PostMapping("/detail")
    public Result<User> detail(@RequestBody IdRequest body) {
        return Result.ok(service.detail(body.getId()));
    }

    @PostMapping("/status")
    @OperationLog(module = "teacher", action = "STATUS")
    public Result<Void> updateStatus(@RequestBody TeacherStatusRequest request) {
        service.updateStatus(request);
        return Result.ok();
    }

    @PostMapping("/reset-password")
    @OperationLog(module = "teacher", action = "RESET_PASSWORD")
    public Result<Void> resetPassword(@RequestBody IdRequest body) {
        service.resetPassword(body.getId());
        return Result.ok();
    }
}
