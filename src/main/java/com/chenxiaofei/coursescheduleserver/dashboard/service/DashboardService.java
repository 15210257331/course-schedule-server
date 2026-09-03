package com.chenxiaofei.coursescheduleserver.dashboard.service;

import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.dashboard.dto.SettlementUpdateRequest;
import com.chenxiaofei.coursescheduleserver.dashboard.entity.Settlement;
import com.chenxiaofei.coursescheduleserver.dashboard.mapper.SettlementMapper;
import com.chenxiaofei.coursescheduleserver.dashboard.mapper.StatMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final StatMapper statMapper;
    private final CourseMapper courseMapper;
    private final SettlementMapper settlementMapper;

    public DashboardService(StatMapper statMapper, CourseMapper courseMapper, SettlementMapper settlementMapper) {
        this.statMapper = statMapper;
        this.courseMapper = courseMapper;
        this.settlementMapper = settlementMapper;
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
        return buildReport(userId, start, end, YearMonth.from(today).toString());
    }

    public Map<String, Object> incomeReport(LocalDate startDate, LocalDate endDate) {
        Long userId = UserContext.getUserId();
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        // 结算月份取区间截止日期所在月，与前端「月份选择」一致
        return buildReport(userId, start, end, YearMonth.from(endDate).toString());
    }

    private Map<String, Object> buildReport(Long userId, LocalDateTime start, LocalDateTime end, String settleMonth) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("trend", statMapper.incomeByDay(userId, start, end));
        result.put("byOrganization", statMapper.incomeByOrganization(userId, start, end));
        result.put("byStudent", statMapper.incomeByStudent(userId, start, end));
        result.put("byStage", statMapper.incomeByStage(userId, start, end));
        result.put("studentFeeDetail", statMapper.feeDetailByStudent(userId, start, end));

        List<Map<String, Object>> orgDetail = statMapper.feeDetailByOrganization(userId, start, end);
        Map<String, Object> settleStat = attachSettlement(userId, settleMonth, orgDetail);
        result.put("organizationFeeDetail", orgDetail);
        result.put("settled", settleStat.get("settled"));
        result.put("unsettled", settleStat.get("unsettled"));

        result.put("total", statMapper.sumIncome(userId, start, end));
        result.put("totalMinutes", statMapper.sumMinutes(userId, start, end) / 60);
        result.put("courseCount", statMapper.countCourses(userId, start, end));
        return result;
    }

    /* 为机构明细行补结清状态，返回该月机构明细中已结 / 未结金额 */
    private Map<String, Object> attachSettlement(Long userId, String settleMonth, List<Map<String, Object>> orgDetail) {
        List<Settlement> sl = settlementMapper.listByMonth(userId, settleMonth);
        Map<String, Boolean> settledMap = new HashMap<>();
        for (Settlement s : sl) {
            settledMap.put(s.getTargetType() + ":" + s.getTargetKey(), Boolean.TRUE.equals(s.getSettled()));
        }
        BigDecimal settledFee = BigDecimal.ZERO;
        BigDecimal unsettledFee = BigDecimal.ZERO;
        for (Map<String, Object> row : orgDetail) {
            String type = String.valueOf(row.get("targetType"));
            String key = String.valueOf(row.get("targetKey"));
            boolean settled = settledMap.getOrDefault(type + ":" + key, false);
            row.put("settled", settled);
            BigDecimal fee = row.get("fee") == null
                    ? BigDecimal.ZERO
                    : new BigDecimal(String.valueOf(row.get("fee")));
            if (settled) {
                settledFee = settledFee.add(fee);
            } else {
                unsettledFee = unsettledFee.add(fee);
            }
        }
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("settled", settledFee);
        stat.put("unsettled", unsettledFee);
        return stat;
    }

    public void updateSettlement(SettlementUpdateRequest request) {
        Long userId = UserContext.getUserId();
        Settlement s = new Settlement();
        s.setUserId(userId);
        s.setSettleMonth(request.getSettleMonth());
        s.setTargetType(request.getTargetType());
        s.setTargetKey(request.getTargetKey());
        s.setSettled(request.getSettled());
        settlementMapper.upsert(s);
    }
}