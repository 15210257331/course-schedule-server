package com.chenxiaofei.coursescheduleserver.coursetemplate.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Pages;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.attachment.service.AttachmentService;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplatePageRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplateRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.entity.CourseTemplate;
import com.chenxiaofei.coursescheduleserver.coursetemplate.mapper.CourseTemplateMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseTemplateService {

    private final CourseTemplateMapper mapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final AttachmentService attachmentService;

    public List<CourseTemplate> list() {
        return list(null);
    }

    public List<CourseTemplate> list(String name) {
        return mapper.listByUser(UserContext.getUserId(), name);
    }

    /** 分页查询（学生姓名模糊），按 id 倒序 */
    public PageResult<CourseTemplate> page(CourseTemplatePageRequest req) {
        Long userId = UserContext.getUserId();
        return Pages.of(req,
                () -> mapper.countByUser(userId, req.getName()),
                (offset, limit) -> mapper.pageByUser(userId, req.getName(), offset, limit));
    }

    public CourseTemplate get(Long id) {
        CourseTemplate t = mapper.findById(id, UserContext.getUserId());
        if (t == null) {
            throw new BusinessException(404, "课程模板不存在");
        }
        return t;
    }

    public CourseTemplate create(CourseTemplateRequest request) {
        CourseTemplate t = new CourseTemplate();
        t.setUserId(UserContext.getUserId());
        apply(t, request);
        checkSubjectAllowed(t.getSubject());
        checkStudentUnique(t.getStudentName(), null);
        mapper.insert(t);
        return get(t.getId());
    }

    @Transactional
    public CourseTemplate update(Long id, CourseTemplateRequest request) {
        get(id);
        CourseTemplate t = new CourseTemplate();
        t.setId(id);
        t.setUserId(UserContext.getUserId());
        apply(t, request);
        checkSubjectAllowed(t.getSubject());
        checkStudentUnique(t.getStudentName(), id);
        mapper.update(t);
        CourseTemplate updated = get(id);
        // 前端要求同步时，把模板最新字段同步到该模板排出的课程（仅同步非重复系列的单节课程）
        if (Boolean.TRUE.equals(request.getSyncCourses())) {
            syncCourses(updated);
        }
        return updated;
    }

    /** 科目必须是当前用户（老师）的任教学科之一；用户未设置任教学科时不限制 */
    private void checkSubjectAllowed(String subject) {
        if (!StringUtils.hasText(subject)) {
            return;
        }
        User user = userMapper.findById(UserContext.getUserId());
        if (user == null || !StringUtils.hasText(user.getSubjects())) {
            return;
        }
        boolean allowed = Arrays.stream(user.getSubjects().split(","))
                .map(String::trim)
                .anyMatch(s -> s.equals(subject));
        if (!allowed) {
            throw new BusinessException(400, "「" + subject + "」不在您的任教学科中，请先到个人中心维护");
        }
    }

    /** 仅删除模板本身（课程保留，但不再关联模板） */
    @Transactional
    public void delete(Long id) {
        get(id);
        Long userId = UserContext.getUserId();
        // 解除已排课程与模板的关联，避免悬空外键
        courseMapper.clearTemplate(userId, id);
        // 级联删除模板附件
        attachmentService.deleteByTemplate(id);
        mapper.delete(id, userId);
    }

    /** 删除模板并连带删除其排出的课程；返回删除的课程数，供前端提示 */
    @Transactional
    public int deleteWithCourses(Long id) {
        get(id);
        Long userId = UserContext.getUserId();
        List<Course> courses = courseMapper.listByTemplate(userId, id);
        courseMapper.deleteByTemplate(userId, id);
        // 级联删除模板附件
        attachmentService.deleteByTemplate(id);
        mapper.delete(id, userId);
        return courses.size();
    }

    /**
     * 把模板最新字段同步到其排出的课程。
     * 重复系列（带 repeatType）的课程随模板快照排课，不随模板后续修改联动，仅同步非重复的单节课程；
     * 同步字段为模板与课程共有的基础字段，不改变课程时间与费用来源。
     */
    private void syncCourses(CourseTemplate t) {
        Long userId = UserContext.getUserId();
        for (Course c : courseMapper.listByTemplate(userId, t.getId())) {
            if (c.getRepeatType() != null && !c.getRepeatType().isBlank()) {
                continue;
            }
            c.setTitle(t.getTitle());
            c.setStudentName(t.getStudentName());
            c.setOrganizationId(t.getOrganizationId());
            c.setSubject(t.getSubject());
            c.setStage(t.getStage());
            c.setCourseType(t.getCourseType());
            c.setLocation(t.getLocation());
            c.setNote(t.getNote());
            // 时长同步：以课程原开始时间为准，按模板最新时长重算结束时间
            if (t.getDurationMinutes() != null && c.getStartTime() != null) {
                c.setEndTime(c.getStartTime().plusMinutes(t.getDurationMinutes()));
            }
            courseMapper.update(c);
        }
    }

    /**
     * 一个学生只能有一个课程模板（模板即学生，按学生姓名唯一）
     */
    private void checkStudentUnique(String studentName, Long excludeId) {
        if (!StringUtils.hasText(studentName)) {
            return;
        }
        int count = mapper.countByStudentName(UserContext.getUserId(), studentName.trim(), excludeId);
        if (count > 0) {
            throw new BusinessException(400, "该学生已有课程模板，一个学生只能有一个模板");
        }
    }

    private void apply(CourseTemplate t, CourseTemplateRequest request) {
        t.setTitle(request.getTitle());
        t.setStudentName(request.getStudentName());
        t.setOrganizationId(request.getOrganizationId());
        t.setSubject(request.getSubject());
        t.setStage(request.getStage());
        t.setCourseType(request.getCourseType());
        t.setDurationMinutes(request.getDurationMinutes());
        t.setFee(request.getFee());
        t.setLocation(request.getLocation());
        t.setNote(request.getNote());
        t.setRepeatType(request.getRepeatType());
    }
}
