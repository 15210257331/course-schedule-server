package com.chenxiaofei.coursescheduleserver.dashboard.service;

import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.dashboard.mapper.StatMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
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
        LocalDateTime weekStart = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime yearStart = today.withDayOfYear(1).atStartOfDay();

        List<Course> todayCourses = courseMapper.listInRange(userId, dayStart, dayEnd);
        long upcoming = todayCourses.stream()
                .filter(c -> "scheduled".equals(c.getStatus()) && c.getStartTime().isAfter(LocalDateTime.now()))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayIncome", statMapper.sumIncome(userId, dayStart, dayEnd));
        result.put("weekIncome", statMapper.sumIncome(userId, weekStart, dayEnd));
        result.put("monthIncome", statMapper.sumIncome(userId, monthStart, dayEnd));
        result.put("yearIncome", statMapper.sumIncome(userId, yearStart, dayEnd));
        result.put("todayCourseCount", todayCourses.size());
        result.put("upcomingCourseCount", upcoming);
        result.put("weekMinutes", statMapper.sumMinutes(userId, weekStart, dayEnd) / 60);
        result.put("monthCourseCount", statMapper.countCourses(userId, monthStart, dayEnd));
        return result;
    }

    public List<Course> todayCourses() {
        Long userId = UserContext.getUserId();
        LocalDate today = LocalDate.now();
        return courseMapper.listInRange(userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    public Map<String, Object> incomeReport(int days) {
        Long userId = UserContext.getUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(days - 1L).atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", statMapper.incomeByDay(userId, start, end));
        result.put("byOrganization", statMapper.incomeByOrganization(userId, start, end));
        result.put("byStudent", statMapper.incomeByStudent(userId, start, end));
        result.put("bySubject", statMapper.incomeBySubject(userId, start, end));
        result.put("total", statMapper.sumIncome(userId, start, end));
        result.put("totalMinutes", statMapper.sumMinutes(userId, start, end) / 60);
        result.put("courseCount", statMapper.countCourses(userId, start, end));
        return result;
    }

    public BigDecimal incomeBetween(LocalDateTime start, LocalDateTime end) {
        return statMapper.sumIncome(UserContext.getUserId(), start, end);
    }
}