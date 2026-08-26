package com.chenxiaofei.coursescheduleserver.snapshot.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.salaryrule.entity.SalaryRule;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.course.service.CourseService;
import com.chenxiaofei.coursescheduleserver.organization.service.OrganizationService;
import com.chenxiaofei.coursescheduleserver.salaryrule.service.SalaryRuleService;
import com.chenxiaofei.coursescheduleserver.student.service.StudentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 快照接口：一次性拉取全部业务数据，前端落 IndexedDB 实现极速启动
 */
@RestController
@RequestMapping("/api/snapshot")
public class SnapshotController {

    private final CourseService courseService;
    private final StudentService studentService;
    private final OrganizationService organizationService;
    private final SalaryRuleService salaryRuleService;

    public SnapshotController(CourseService courseService, StudentService studentService,
                              OrganizationService organizationService, SalaryRuleService salaryRuleService) {
        this.courseService = courseService;
        this.studentService = studentService;
        this.organizationService = organizationService;
        this.salaryRuleService = salaryRuleService;
    }

    @GetMapping
    public Result<Map<String, Object>> snapshot() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusMonths(6).withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = today.plusMonths(6).withDayOfMonth(1).atStartOfDay();

        List<Course> courses = courseService.listInRange(start, end);
        List<Student> students = studentService.list();
        List<Organization> organizations = organizationService.list();
        List<SalaryRule> rules = salaryRuleService.list();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courses", courses);
        result.put("students", students);
        result.put("organizations", organizations);
        result.put("salaryRules", rules);
        result.put("userId", UserContext.getUserId());
        result.put("generatedAt", LocalDateTime.now().toString());
        return Result.ok(result);
    }
}