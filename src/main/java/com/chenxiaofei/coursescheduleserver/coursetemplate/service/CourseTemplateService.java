package com.chenxiaofei.coursescheduleserver.coursetemplate.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplatePageRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplateRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.entity.CourseTemplate;
import com.chenxiaofei.coursescheduleserver.coursetemplate.mapper.CourseTemplateMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.student.mapper.StudentMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class CourseTemplateService {

    private final CourseTemplateMapper mapper;
    private final StudentMapper studentMapper;
    private final UserMapper userMapper;

    public CourseTemplateService(CourseTemplateMapper mapper, StudentMapper studentMapper, UserMapper userMapper) {
        this.mapper = mapper;
        this.studentMapper = studentMapper;
        this.userMapper = userMapper;
    }

    public List<CourseTemplate> list() {
        return list(null);
    }

    public List<CourseTemplate> list(String name) {
        return mapper.listByUser(UserContext.getUserId(), name);
    }

    /** 分页查询（学生姓名模糊），按 id 倒序 */
    public PageResult<CourseTemplate> page(CourseTemplatePageRequest req) {
        Long userId = UserContext.getUserId();
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 20 : req.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;
        long total = mapper.countByUser(userId, req.getName());
        List<CourseTemplate> list = mapper.pageByUser(userId, req.getName(), offset, pageSize);
        return PageResult.of(total, list);
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
        resolveStudent(t);
        checkStudentUnique(t.getStudentId(), null);
        mapper.insert(t);
        return get(t.getId());
    }

    public CourseTemplate update(Long id, CourseTemplateRequest request) {
        get(id);
        CourseTemplate t = new CourseTemplate();
        t.setId(id);
        t.setUserId(UserContext.getUserId());
        apply(t, request);
        checkSubjectAllowed(t.getSubject());
        resolveStudent(t);
        checkStudentUnique(t.getStudentId(), id);
        mapper.update(t);
        return get(id);
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

    public void delete(Long id) {
        get(id);
        mapper.delete(id, UserContext.getUserId());
    }

    /**
     * 学生字段为输入框：按姓名查 student 表，存在则绑定 student_id 并同步机构/科目；
     * 不存在则自动建档。无姓名时按原样存 student_id（可能为 null）。
     */
    private void resolveStudent(CourseTemplate t) {
        Long userId = UserContext.getUserId();
        if (StringUtils.hasText(t.getStudentName())) {
            String name = t.getStudentName().trim();
            Student existing = studentMapper.findByName(userId, name);
            if (existing != null) {
                t.setStudentId(existing.getId());
                t.setStudentName(existing.getName());
                syncStudent(existing, t);
            } else {
                Student s = new Student();
                s.setUserId(userId);
                s.setName(name);
                s.setOrganizationId(t.getOrganizationId());
                s.setSubject(t.getSubject());
                s.setGrade(t.getStage());
                s.setFee(t.getFee());
                studentMapper.insert(s);
                t.setStudentId(s.getId());
            }
            return;
        }
        if (t.getStudentId() != null) {
            Student s = studentMapper.findById(t.getStudentId(), userId);
            if (s != null) {
                t.setStudentName(s.getName());
            }
        }
    }

    /** 模板里的机构/科目/学段同步回学生档案（仅当模板提供了对应值且与档案不同时） */
    private void syncStudent(Student s, CourseTemplate t) {
        boolean dirty = false;
        if (t.getOrganizationId() != null && !t.getOrganizationId().equals(s.getOrganizationId())) {
            s.setOrganizationId(t.getOrganizationId());
            dirty = true;
        }
        if (StringUtils.hasText(t.getSubject()) && !t.getSubject().equals(s.getSubject())) {
            s.setSubject(t.getSubject());
            dirty = true;
        }
        // 模板学段（初一~高三）同步为学员档案 grade
        if (StringUtils.hasText(t.getStage()) && !t.getStage().equals(s.getGrade())) {
            s.setGrade(t.getStage());
            dirty = true;
        }
        if (dirty) {
            studentMapper.update(s);
        }
    }

    /** 一个学生只能有一个课程模板 */
    private void checkStudentUnique(Long studentId, Long excludeId) {
        if (studentId == null) {
            return;
        }
        int count = mapper.countByStudent(UserContext.getUserId(), studentId, excludeId);
        if (count > 0) {
            throw new BusinessException(400, "该学生已有课程模板，一个学生只能有一个模板");
        }
    }

    private void apply(CourseTemplate t, CourseTemplateRequest request) {
        t.setTitle(request.getTitle());
        t.setStudentId(request.getStudentId());
        t.setStudentName(request.getStudentName());
        t.setOrganizationId(request.getOrganizationId());
        t.setSubject(request.getSubject());
        t.setStage(request.getStage());
        t.setCourseType(request.getCourseType());
        t.setDurationMinutes(request.getDurationMinutes());
        t.setFee(request.getFee());
        t.setFeeManual(request.getFeeManual());
        t.setLocation(request.getLocation());
        t.setNote(request.getNote());
        t.setColor(request.getColor());
        t.setRepeatType(request.getRepeatType());
    }
}
