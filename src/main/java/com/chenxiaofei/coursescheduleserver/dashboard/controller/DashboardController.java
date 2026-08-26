package com.chenxiaofei.coursescheduleserver.dashboard.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(dashboardService.summary());
    }

    @GetMapping("/today-courses")
    public Result<List<Course>> todayCourses() {
        return Result.ok(dashboardService.todayCourses());
    }

    @GetMapping("/income-report")
    public Result<Map<String, Object>> incomeReport(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(dashboardService.incomeReport(days));
    }
}