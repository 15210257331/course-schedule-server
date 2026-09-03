package com.chenxiaofei.coursescheduleserver.dashboard.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.dashboard.dto.SettlementUpdateRequest;
import com.chenxiaofei.coursescheduleserver.dashboard.service.DashboardService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PostMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(dashboardService.summary());
    }

    @PostMapping("/today-courses")
    public Result<List<Course>> todayCourses() {
        return Result.ok(dashboardService.todayCourses());
    }

    @PostMapping("/income-report")
    public Result<Map<String, Object>> incomeReport(@RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            String start = (String) body.get("start");
            String end = (String) body.get("end");
            if (start != null && !start.isBlank() && end != null && !end.isBlank()) {
                return Result.ok(dashboardService.incomeReport(LocalDate.parse(start), LocalDate.parse(end)));
            }
            Object daysObj = body.get("days");
            if (daysObj instanceof Number) {
                return Result.ok(dashboardService.incomeReport(((Number) daysObj).intValue()));
            }
        }
        return Result.ok(dashboardService.incomeReport(30));
    }

    @PutMapping("/settlement")
    public Result<Void> updateSettlement(@Valid @RequestBody SettlementUpdateRequest request) {
        dashboardService.updateSettlement(request);
        return Result.ok();
    }
}