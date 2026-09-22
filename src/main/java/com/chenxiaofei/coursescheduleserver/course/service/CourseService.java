package com.chenxiaofei.coursescheduleserver.course.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Pages;
import com.chenxiaofei.coursescheduleserver.course.dto.CoursePageRequest;
import com.chenxiaofei.coursescheduleserver.course.dto.CourseRequest;
import com.chenxiaofei.coursescheduleserver.course.dto.CopyWeekResult;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.setting.service.SettingService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final SettingService settingService;

    public List<Course> listInRange(LocalDateTime start, LocalDateTime end, String title) {
        return courseMapper.listInRange(UserContext.getUserId(), start, end, title);
    }

    /** 分页查询（title 模糊、时间范围可选），按 start_time 倒序 */
    public PageResult<Course> page(CoursePageRequest req) {
        Long userId = UserContext.getUserId();
        return Pages.of(req,
                () -> courseMapper.countInRange(userId, req.getStart(), req.getEnd(), req.getTitle()),
                (offset, limit) -> courseMapper.pageInRange(userId, req.getStart(), req.getEnd(), req.getTitle(), offset, limit));
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
        lockUser();
        try {
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
        } finally {
            unlockUser();
        }
    }

    @Transactional
    public Course update(Long id, CourseRequest request) {
        lockUser();
        try {
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
        } finally {
            unlockUser();
        }
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
        req.setStudentName(src.getStudentName());
        req.setOrganizationId(src.getOrganizationId());
        req.setSubject(src.getSubject());
        req.setStage(src.getStage());
        req.setCourseType(src.getCourseType());
        req.setStartTime(newStart);
        req.setEndTime(newEnd);
        req.setFee(src.getFee());
        req.setLocation(src.getLocation());
        req.setNote(src.getNote());
        req.setStatus("scheduled");
        req.setColor(src.getColor());
        req.setReminderOffsetMinutes(src.getReminderOffsetMinutes());
        return create(req);
    }

    @Transactional
    public Course move(Long id, LocalDateTime newStart, LocalDateTime newEnd) {
        lockUser();
        try {
            get(id);
            validateTime(newStart, newEnd);
            checkConflict(newStart, newEnd, id);
            courseMapper.updateTime(id, UserContext.getUserId(), newStart, newEnd);
            return get(id);
        } finally {
            unlockUser();
        }
    }

    /**
     * 复制指定周的全部课程到目标周。
     * sourceMonday 为空时默认取本周周一；targetWeek 为相对该源周往前/后的偏移量（1=下一周）。
     * 已结束的课程也一并复制（用户可能想延续排课）；仅跳过重复系列课与目标时段冲突的课。
     * 返回复制数 + 各类跳过数，供前端给出「已复制 N 节，跳过 M 节」提示。
     */
    @Transactional
    public CopyWeekResult copyWeekTo(String sourceMondayStr, int targetWeek) {
        lockUser();
        try {
            Long userId = UserContext.getUserId();
            LocalDate sourceMonday = parseMonday(sourceMondayStr);
            LocalDate targetMonday = sourceMonday.plusWeeks(targetWeek);
            long days = Duration.between(sourceMonday.atStartOfDay(), targetMonday.atStartOfDay()).toDays();

            List<Course> sources = courseMapper.listInRange(userId,
                    sourceMonday.atTime(LocalTime.MIN), sourceMonday.plusDays(7).atTime(LocalTime.MIN), null);
            CopyWeekResult result = new CopyWeekResult();
            for (Course src : sources) {
                // 重复系列由模板/手动重复生成源，不参与整周复制，避免重复叠加
                if (src.getRepeatType() != null && !src.getRepeatType().isBlank()) {
                    result.setSkippedRepeat(result.getSkippedRepeat() + 1);
                    continue;
                }
                LocalDateTime newStart = src.getStartTime().plusDays(days);
                LocalDateTime newEnd = src.getEndTime().plusDays(days);
                // 目标时段已有课则跳过，避免整周重叠
                if (courseMapper.countConflict(userId, newStart, newEnd) > 0) {
                    result.setSkippedConflict(result.getSkippedConflict() + 1);
                    continue;
                }
                CourseRequest req = toRequest(src);
                req.setStartTime(newStart);
                req.setEndTime(newEnd);
                courseMapper.insert(buildFrom(req, userId, src.getFee()));
                result.setCopied(result.getCopied() + 1);
            }
            return result;
        } finally {
            unlockUser();
        }
    }

    private LocalDate parseMonday(String sourceMondayStr) {
        if (sourceMondayStr != null && !sourceMondayStr.isBlank()) {
            try {
                LocalDate parsed = LocalDate.parse(sourceMondayStr.trim());
                return parsed.with(DayOfWeek.MONDAY);
            } catch (Exception ignored) {
            }
        }
        return LocalDate.now().with(DayOfWeek.MONDAY);
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
                next.setStudentName(c.getStudentName());
                next.setOrganizationId(c.getOrganizationId());
                next.setSubject(c.getSubject());
                next.setStage(c.getStage());
                next.setCourseType(c.getCourseType());
                next.setStartTime(nextStart);
                next.setEndTime(nextEnd);
                next.setFee(c.getFee());
                next.setLocation(c.getLocation());
                next.setNote(c.getNote());
                next.setStatus("scheduled");
                next.setColor(c.getColor());
                next.setReminderOffsetMinutes(c.getReminderOffsetMinutes());
                next.setRepeatType(repeatType);
                next.setRepeatEndDate(repeatEnd);
                next.setParentId(c.getId());
                next.setTemplateId(c.getTemplateId());
                courseMapper.insert(next);
            }
        }
    }

    private CourseRequest toRequest(Course src) {
        CourseRequest req = new CourseRequest();
        req.setTitle(src.getTitle());
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

    private Course buildFrom(CourseRequest request, Long userId, BigDecimal fee) {
        Course c = new Course();
        c.setUserId(userId);
        apply(c, request);
        c.setFee(fee);
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
        c.setStudentName(request.getStudentName());
        c.setOrganizationId(request.getOrganizationId());
        c.setSubject(request.getSubject());
        c.setStage(request.getStage());
        c.setCourseType(request.getCourseType());
        c.setStartTime(request.getStartTime());
        c.setEndTime(request.getEndTime());
        c.setFee(request.getFee());
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
        if (request.getTemplateId() != null) {
            c.setTemplateId(request.getTemplateId());
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

    /**
     * 按 user_id 加 MySQL 会话级 advisory lock，串行化同一教师的课程写操作。
     * 消除「先 countConflict 再 insert」的 TOCTOU 并发竞态：两个并发请求各自读到 count=0 后都插入重叠课程。
     * 锁按 user_id 粒度，不同教师互不阻塞；GET_LOCK 是 session 级，连接池复用连接不会自动释放，必须 finally 显式释放。
     */
    private void lockUser() {
        Integer ok = courseMapper.getLock(lockName(), 10);
        if (ok == null || ok != 1) {
            throw new BusinessException(409, "操作繁忙，请稍后重试");
        }
    }

    private void unlockUser() {
        try {
            courseMapper.releaseLock(lockName());
        } catch (Exception ignored) {
            // 释放失败不影响业务；锁会在连接归还后由 GET_LOCK 语义自动失效或被下次 GET_LOCK 重置
        }
    }

    private String lockName() {
        return "course:user:" + UserContext.getUserId();
    }
}