package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.admin.service.AdminStatsService;
import com.chenxiaofei.coursescheduleserver.common.Result;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminStatsService service;

    @PostMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestBody(required = false) Map<String, Integer> body) {
        int days = (body != null && body.get("days") != null) ? body.get("days") : 30;
        return Result.ok(service.overview(days));
    }
}
