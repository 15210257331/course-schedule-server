package com.chenxiaofei.coursescheduleserver.dashboard.service;

import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.dashboard.mapper.StatMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final StatMapper statMapper;
    private final CourseMapper courseMapper;

    public DashboardService(StatMapper statMapper, CourseMapper courseMapper) {
        this.statMapper = statMapper;
        this.courseMapper = courseMapper;
    }

    public Map<String, Object> summary() {
        Long userId = UserContext.getUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime lastMonthStart = today.minusMonths(1).withDayOfMonth(1).atStartOfDay();
        LocalDateTime lastMonthEnd = monthStart; // 本月首日 = 上月末日之后

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayIncome", statMapper.sumIncome(userId, dayStart, dayEnd));
        result.put("lastMonthIncome", statMapper.sumIncome(userId, lastMonthStart, lastMonthEnd));
        result.put("monthIncome", statMapper.sumIncome(userId, monthStart, dayEnd));
        result.put("yearIncome", statMapper.sumIncome(userId, yearStart, dayEnd));
        return result;
    }

    public List<Course> todayCourses() {
        Long userId = UserContext.getUserId();
        LocalDate today = LocalDate.now();
        return courseMapper.listInRange(userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay(), null);
    }

    public Map<String, Object> incomeReport(int days) {
        Long userId = UserContext.getUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return buildReport(userId, start, end);
    }

    public Map<String, Object> incomeReport(LocalDate startDate, LocalDate endDate) {
        Long userId = UserContext.getUserId();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        return buildReport(userId, start, end);
    }

    private Map<String, Object> buildReport(Long userId, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", statMapper.incomeByDay(userId, start, end));
        result.put("byOrganization", statMapper.incomeByOrganization(userId, start, end));
        result.put("byStudent", statMapper.incomeByStudent(userId, start, end));
        result.put("byStage", statMapper.incomeByStage(userId, start, end));
        result.put("studentFeeDetail", statMapper.feeDetailByStudent(userId, start, end));
        result.put("organizationFeeDetail", statMapper.feeDetailByOrganization(userId, start, end));
        result.put("total", statMapper.sumIncome(userId, start, end));
        result.put("totalMinutes", statMapper.sumMinutes(userId, start, end) / 60);
        result.put("courseCount", statMapper.countCourses(userId, start, end));
        return result;
    }
}