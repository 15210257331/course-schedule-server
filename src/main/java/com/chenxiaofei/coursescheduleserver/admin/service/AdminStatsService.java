package com.chenxiaofei.coursescheduleserver.admin.service;

import com.chenxiaofei.coursescheduleserver.admin.entity.AdminMessage;
import com.chenxiaofei.coursescheduleserver.admin.mapper.AdminStatsMapper;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端数据看板
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final AdminStatsMapper statsMapper;
    private final UserMapper userMapper;

    /** 看板总览：统计卡片 + 近 N 天趋势 + 最新动态 */
    public Map<String, Object> overview(int days) {
        int d = Math.min(Math.max(days, 7), 90);
        LocalDate today = LocalDate.now();
        LocalDate trendStart = today.minusDays(d - 1L);
        LocalDate monthStart = today.withDayOfMonth(1);

        Map<String, Object> result = new HashMap<>();

        // ---- 统计卡片 ----
        Map<String, Object> cards = new HashMap<>();
        long teacherTotal = statsMapper.countTeachers();
        long activeTeachers = userMapper.countTeachers("active", null);
        long disabledTeachers = teacherTotal - activeTeachers;
        long messageTotal = statsMapper.countPublishedMessages();
        long readTotal = statsMapper.countMessageReads();
        // 目标总人次 = Σ 每条消息的目标人数（all 按教师总数近似）
        long targetTotal = estimateTargetTotal(teacherTotal);

        cards.put("teacherTotal", teacherTotal);
        cards.put("teacherActive", activeTeachers);
        cards.put("teacherDisabled", disabledTeachers);
        cards.put("teacherNewToday", statsMapper.countNewTeachers(today));
        cards.put("teacherNewMonth", statsMapper.countNewTeachers(monthStart));
        cards.put("courseTotal", statsMapper.countCourses());
        cards.put("courseToday", statsMapper.countCoursesInRange(today, today));
        cards.put("courseMonth", statsMapper.countCoursesInRange(monthStart, today));
        cards.put("messageTotal", messageTotal);
        cards.put("readTotal", readTotal);
        cards.put("targetTotal", targetTotal);
        cards.put("readRate", targetTotal > 0 ? Math.round(readTotal * 10000.0 / targetTotal) / 100.0 : 0);
        result.put("cards", cards);

        // ---- 趋势图（按天补齐） ----
        result.put("teacherTrend", fillDays(trendStart, today, statsMapper.teacherTrend(trendStart, today)));
        result.put("courseTrend", fillDays(trendStart, today, statsMapper.courseTrend(trendStart, today)));

        // ---- 最新动态 ----
        List<AdminMessage> recentMessages = statsMapper.recentMessages(5);
        for (AdminMessage m : recentMessages) {
            m.setTargetCount("all".equals(m.getTargetType())
                    ? (int) teacherTotal
                    : (m.getTargetIds() == null || m.getTargetIds().isBlank() ? 0 : m.getTargetIds().split(",").length));
        }
        result.put("recentMessages", recentMessages);

        List<User> recentTeachers = statsMapper.recentTeachers(6);
        for (User u : recentTeachers) {
            u.setPassword(null);
        }
        result.put("recentTeachers", recentTeachers);

        return result;
    }

    /** 目标总人次估算：all 消息 × 教师总数 + specific 消息 × 指定人数 */
    private long estimateTargetTotal(long teacherTotal) {
        List<AdminMessage> all = statsMapper.recentMessages(500);
        long sum = 0;
        for (AdminMessage m : all) {
            if ("all".equals(m.getTargetType())) {
                sum += teacherTotal;
            } else if (m.getTargetIds() != null && !m.getTargetIds().isBlank()) {
                sum += m.getTargetIds().split(",").length;
            }
        }
        return sum;
    }

    /** 按天补齐 0 值，返回 [{day, count}] */
    private List<Map<String, Object>> fillDays(LocalDate start, LocalDate end, List<Map<String, Object>> rows) {
        Map<String, Long> byDay = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object day = row.get("day");
            Object count = row.get("count");
            if (day != null) {
                byDay.put(day.toString(), count == null ? 0L : ((Number) count).longValue());
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Map<String, Object> item = new HashMap<>();
            item.put("day", d.toString());
            item.put("count", byDay.getOrDefault(d.toString(), 0L));
            out.add(item);
        }
        return out;
    }
}
