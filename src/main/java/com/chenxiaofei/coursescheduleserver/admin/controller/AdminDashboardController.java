package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.admin.service.AdminStatsService;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.security.AdminGuard;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端 - 数据看板
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminStatsService service;
    private final AdminGuard adminGuard;

    public AdminDashboardController(AdminStatsService service, AdminGuard adminGuard) {
        this.service = service;
        this.adminGuard = adminGuard;
    }

    @PostMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestBody(required = false) Map<String, Integer> body) {
        adminGuard.requireAdmin();
        int days = (body != null && body.get("days") != null) ? body.get("days") : 30;
        return Result.ok(service.overview(days));
    }
}
