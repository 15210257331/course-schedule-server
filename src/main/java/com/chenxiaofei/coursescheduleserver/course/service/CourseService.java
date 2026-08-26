package com.chenxiaofei.coursescheduleserver.course.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.course.dto.CourseRequest;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.salaryrule.entity.SalaryRule;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.salaryrule.mapper.SalaryRuleMapper;
import com.chenxiaofei.coursescheduleserver.student.mapper.StudentMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final SalaryRuleMapper salaryRuleMapper;
    private final StudentMapper studentMapper;

    public CourseService(CourseMapper courseMapper, SalaryRuleMapper salaryRuleMapper, StudentMapper studentMapper) {
        this.courseMapper = courseMapper;
        this.salaryRuleMapper = salaryRuleMapper;
        this.studentMapper = studentMapper;
    }

    public List<Course> listInRange(LocalDateTime start, LocalDateTime end) {
        return courseMapper.listInRange(UserContext.getUserId(), start, end);
    }

    public Course get(Long id) {
        Course c = courseMapper.findById(id, UserContext.getUserId());
        if (c == null) {
            throw new BusinessException(404, "课程不存在");
        }
        return c;
    }

    @Transactional
    public Course create(CourseRequest request) {
        validateTime(request.getStartTime(), request.getEndTime());
        checkConflict(request.getStartTime(), request.getEndTime(), null);

        Course c = new Course();
        c.setUserId(UserContext.getUserId());
        apply(c, request);
        resolveFee(c);
        if (c.getStatus() == null) {
            c.setStatus("scheduled");
        }
        if (c.getColor() == null || c.getColor().isBlank()) {
            c.setColor("#409EFF");
        }
        courseMapper.insert(c);

        handleRepeatAfter(c, request);
        return get(c.getId());
    }

    @Transactional
    public Course update(Long id, CourseRequest request) {
        Course exist = get(id);
        validateTime(request.getStartTime(), request.getEndTime());
        checkConflict(request.getStartTime(), request.getEndTime(), id);

        Course c = new Course();
        c.setId(id);
        c.setUserId(UserContext.getUserId());
        apply(c, request);
        c.setStatus(request.getStatus() == null ? exist.getStatus() : request.getStatus());
        resolveFee(c);
        courseMapper.update(c);
        return get(id);
    }

    public void delete(Long id) {
        get(id);
        Long userId = UserContext.getUserId();
        courseMapper.delete(id, userId);
        if (courseMapper.countByParent(userId, id) > 0) {
            // 删除主课程时连带删除其重复生成的课程
            deleteRepeatSeries(id);
        }
    }

    private void deleteRepeatSeries(Long parentId) {
        Long userId = UserContext.getUserId();
        for (Course child : courseMapper.listInRange(userId, LocalDateTime.of(1900, 1, 1, 0, 0),
                LocalDateTime.of(2100, 1, 1, 0, 0))) {
            if (parentId.equals(child.getParentId())) {
                courseMapper.delete(child.getId(), userId);
            }
        }
    }

    @Transactional
    public Course copy(Long id, LocalDateTime newStart, LocalDateTime newEnd) {
        Course src = get(id);
        CourseRequest req = new CourseRequest();
        req.setTitle(src.getTitle());
        req.setStudentId(src.getStudentId());
        req.setOrganizationId(src.getOrganizationId());
        req.setSubject(src.getSubject());
        req.setCourseType(src.getCourseType());
        req.setStartTime(newStart);
        req.setEndTime(newEnd);
        req.setFee(src.getFee());
        req.setFeeManual(true);
        req.setLocation(src.getLocation());
        req.setNote(src.getNote());
        req.setStatus("scheduled");
        req.setColor(src.getColor());
        req.setReminderOffsetMinutes(src.getReminderOffsetMinutes());
        return create(req);
    }

    @Transactional
    public Course move(Long id, LocalDateTime newStart, LocalDateTime newEnd) {
        get(id);
        validateTime(newStart, newEnd);
        checkConflict(newStart, newEnd, id);
        courseMapper.updateTime(id, UserContext.getUserId(), newStart, newEnd);
        return get(id);
    }

    /** 复制指定周（如：下周）的全部课程 */
    @Transactional
    public int copyWeekTo(int targetWeek) {
        Long userId = UserContext.getUserId();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate targetMonday = monday.plusWeeks(targetWeek);

        List<Course> sources = courseMapper.listInRange(userId,
                monday.atTime(LocalTime.MIN), monday.plusDays(7).atTime(LocalTime.MIN));
        int count = 0;
        for (Course src : sources) {
            if (src.getRepeatType() != null && !src.getRepeatType().isBlank()) {
                continue;
            }
            long days = Duration.between(monday.atStartOfDay(), targetMonday.atStartOfDay()).toDays();
            CourseRequest req = toRequest(src);
            req.setStartTime(src.getStartTime().plusDays(days));
            req.setEndTime(src.getEndTime().plusDays(days));
            req.setFeeManual(true);
            courseMapper.insert(buildFrom(req, userId, src.getFee(), true));
            count++;
        }
        return count;
    }

    private void handleRepeatAfter(Course c, CourseRequest request) {
        String repeatType = request.getRepeatType();
        LocalDate repeatEnd = request.getRepeatEndDate();
        if (repeatType == null || repeatType.isBlank() || repeatEnd == null) {
            return;
        }
        LocalDate current = c.getStartTime().toLocalDate();
        while (current.isBefore(repeatEnd)) {
            switch (repeatType) {
                case "daily" -> current = current.plusDays(1);
                case "weekly" -> current = current.plusWeeks(1);
                case "monthly" -> current = current.plusMonths(1);
                default -> current = repeatEnd;
            }
            if (!current.isAfter(repeatEnd)) {
                Course next = new Course();
                next.setUserId(c.getUserId());
                next.setTitle(c.getTitle());
                next.setStudentId(c.getStudentId());
                next.setOrganizationId(c.getOrganizationId());
                next.setSubject(c.getSubject());
                next.setCourseType(c.getCourseType());
                next.setStartTime(LocalDateTime.of(current, c.getStartTime().toLocalTime()));
                next.setEndTime(LocalDateTime.of(current, c.getEndTime().toLocalTime()));
                next.setFee(c.getFee());
                next.setFeeManual(c.getFeeManual());
                next.setLocation(c.getLocation());
                next.setNote(c.getNote());
                next.setStatus("scheduled");
                next.setColor(c.getColor());
                next.setReminderOffsetMinutes(c.getReminderOffsetMinutes());
                next.setRepeatType(repeatType);
                next.setRepeatEndDate(repeatEnd);
                next.setParentId(c.getId());
                courseMapper.insert(next);
            }
        }
    }

    private CourseRequest toRequest(Course src) {
        CourseRequest req = new CourseRequest();
        req.setTitle(src.getTitle());
        req.setStudentId(src.getStudentId());
        req.setOrganizationId(src.getOrganizationId());
        req.setSubject(src.getSubject());
        req.setCourseType(src.getCourseType());
        req.setStartTime(src.getStartTime());
        req.setEndTime(src.getEndTime());
        req.setFee(src.getFee());
        req.setLocation(src.getLocation());
        req.setNote(src.getNote());
        req.setStatus("scheduled");
        req.setColor(src.getColor());
        req.setReminderOffsetMinutes(src.getReminderOffsetMinutes());
        return req;
    }

    private Course buildFrom(CourseRequest request, Long userId, BigDecimal fee, boolean feeManual) {
        Course c = new Course();
        c.setUserId(userId);
        apply(c, request);
        c.setFee(fee);
        c.setFeeManual(feeManual);
        if (c.getStatus() == null) {
            c.setStatus("scheduled");
        }
        if (c.getColor() == null || c.getColor().isBlank()) {
            c.setColor("#409EFF");
        }
        return c;
    }

    private void apply(Course c, CourseRequest request) {
        c.setTitle(request.getTitle());
        c.setStudentId(request.getStudentId());
        c.setOrganizationId(request.getOrganizationId());
        c.setSubject(request.getSubject());
        c.setCourseType(request.getCourseType());
        c.setStartTime(request.getStartTime());
        c.setEndTime(request.getEndTime());
        c.setFee(request.getFee());
        c.setFeeManual(request.getFeeManual() != null && request.getFeeManual());
        c.setLocation(request.getLocation());
        c.setNote(request.getNote());
        c.setStatus(request.getStatus());
        c.setColor(request.getColor());
        c.setReminderOffsetMinutes(request.getReminderOffsetMinutes() == null
                ? 30 : request.getReminderOffsetMinutes());
        if (request.getRepeatType() != null) {
            c.setRepeatType(request.getRepeatType());
            c.setRepeatEndDate(request.getRepeatEndDate());
        }
        if (request.getParentId() != null) {
            c.setParentId(request.getParentId());
        }
    }

    /** 未手动指定课时费时，按 收费规则(机构+年级+科目) 自动匹配 */
    private void resolveFee(Course c) {
        boolean manual = c.getFeeManual() != null && c.getFeeManual();
        if (manual) {
            return;
        }
        if (c.getFee() != null && c.getFee().compareTo(BigDecimal.ZERO) > 0) {
            // 前端直接给了课时费就沿用（按小时折算的规则见下）
            return;
        }
        Long orgId = c.getOrganizationId();
        String subject = c.getSubject();
        String grade = null;
        if (c.getStudentId() != null) {
            Student student = studentMapper.findById(c.getStudentId(), UserContext.getUserId());
            if (student != null) {
                grade = student.getGrade();
            }
        }
        SalaryRule rule = salaryRuleMapper.match(UserContext.getUserId(), orgId, grade, subject);
        if (rule == null || c.getStartTime() == null || c.getEndTime() == null) {
            return;
        }
        BigDecimal hours = BigDecimal.valueOf(Duration.between(c.getStartTime(), c.getEndTime()).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        c.setFee(rule.getHourlyFee().multiply(hours).setScale(2, RoundingMode.HALF_UP));
    }

    private void validateTime(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BusinessException(400, "课程时间不能为空");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException(400, "结束时间必须晚于开始时间");
        }
    }

    private void checkConflict(LocalDateTime start, LocalDateTime end, Long excludeId) {
        int count = excludeId == null
                ? courseMapper.countConflict(UserContext.getUserId(), start, end)
                : courseMapper.countConflictExclude(UserContext.getUserId(), start, end, excludeId);
        if (count > 0) {
            throw new BusinessException(409, "该时间段已存在其他课程，请调整时间");
        }
    }
}