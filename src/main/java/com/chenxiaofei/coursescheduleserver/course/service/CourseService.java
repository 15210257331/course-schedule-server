package com.chenxiaofei.coursescheduleserver.course.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.course.dto.CoursePageRequest;
import com.chenxiaofei.coursescheduleserver.course.dto.CourseRequest;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.setting.service.SettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final SettingService settingService;

    public CourseService(CourseMapper courseMapper, SettingService settingService) {
        this.courseMapper = courseMapper;
        this.settingService = settingService;
    }

    public List<Course> listInRange(LocalDateTime start, LocalDateTime end, String title) {
        return courseMapper.listInRange(UserContext.getUserId(), start, end, title);
    }

    /** 分页查询（title 模糊、时间范围可选），按 start_time 倒序 */
    public PageResult<Course> page(CoursePageRequest req) {
        Long userId = UserContext.getUserId();
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 20 : req.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;
        long total = courseMapper.countInRange(userId, req.getStart(), req.getEnd(), req.getTitle());
        List<Course> list = courseMapper.pageInRange(userId, req.getStart(), req.getEnd(), req.getTitle(), offset, pageSize);
        return PageResult.of(total, list);
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
                LocalDateTime.of(2100, 1, 1, 0, 0), null)) {
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
        req.setStudentName(src.getStudentName());
        req.setOrganizationId(src.getOrganizationId());
        req.setSubject(src.getSubject());
        req.setStage(src.getStage());
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

    /** 复制指定周（如：下周）的全部课程（跳过已结束课程，并做目标周冲突校验） */
    @Transactional
    public int copyWeekTo(int targetWeek) {
        Long userId = UserContext.getUserId();
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate targetMonday = monday.plusWeeks(targetWeek);
        long days = Duration.between(monday.atStartOfDay(), targetMonday.atStartOfDay()).toDays();

        List<Course> sources = courseMapper.listInRange(userId,
                monday.atTime(LocalTime.MIN), monday.plusDays(7).atTime(LocalTime.MIN), null);
        int count = 0;
        for (Course src : sources) {
            // 重复系列由模板/手动重复生成源，不参与整周复制，避免重复叠加
            if (src.getRepeatType() != null && !src.getRepeatType().isBlank()) {
                continue;
            }
            // 已结束的课程不复刻到下周
            if (src.getEndTime().isBefore(LocalDateTime.now())) {
                continue;
            }
            LocalDateTime newStart = src.getStartTime().plusDays(days);
            LocalDateTime newEnd = src.getEndTime().plusDays(days);
            // 目标时段已有课则跳过，避免整周重叠
            if (courseMapper.countConflict(userId, newStart, newEnd) > 0) {
                continue;
            }
            CourseRequest req = toRequest(src);
            req.setStartTime(newStart);
            req.setEndTime(newEnd);
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
                case "biweekly" -> current = current.plusWeeks(2);
                default -> current = repeatEnd;
            }
            if (!current.isAfter(repeatEnd)) {
                LocalDateTime nextStart = LocalDateTime.of(current, c.getStartTime().toLocalTime());
                LocalDateTime nextEnd = LocalDateTime.of(current, c.getEndTime().toLocalTime());
                // 逐节冲突校验：目标时段已有课则跳过该节，不整批失败
                if (courseMapper.countConflict(c.getUserId(), nextStart, nextEnd) > 0) {
                    continue;
                }
                Course next = new Course();
                next.setUserId(c.getUserId());
                next.setTitle(c.getTitle());
                next.setStudentId(c.getStudentId());
                next.setStudentName(c.getStudentName());
                next.setOrganizationId(c.getOrganizationId());
                next.setSubject(c.getSubject());
                next.setStage(c.getStage());
                next.setCourseType(c.getCourseType());
                next.setStartTime(nextStart);
                next.setEndTime(nextEnd);
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
        req.setStudentName(src.getStudentName());
        req.setOrganizationId(src.getOrganizationId());
        req.setSubject(src.getSubject());
        req.setStage(src.getStage());
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
        c.setStudentName(request.getStudentName());
        c.setOrganizationId(request.getOrganizationId());
        c.setSubject(request.getSubject());
        c.setStage(request.getStage());
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
                ? getDefaultReminderOffset() : request.getReminderOffsetMinutes());
        if (request.getRepeatType() != null) {
            c.setRepeatType(request.getRepeatType());
            c.setRepeatEndDate(request.getRepeatEndDate());
        }
        if (request.getParentId() != null) {
            c.setParentId(request.getParentId());
        }
    }

    private int getDefaultReminderOffset() {
        try {
            String value = settingService.list().get("reminderOffset");
            return value != null ? Integer.parseInt(value) : 30;
        } catch (Exception e) {
            return 30;
        }
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