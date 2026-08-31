package com.chenxiaofei.coursescheduleserver.student.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.coursetemplate.mapper.CourseTemplateMapper;
import com.chenxiaofei.coursescheduleserver.student.dto.StudentRequest;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.student.mapper.StudentMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentMapper mapper;
    private final CourseMapper courseMapper;
    private final CourseTemplateMapper courseTemplateMapper;

    public StudentService(StudentMapper mapper, CourseMapper courseMapper, CourseTemplateMapper courseTemplateMapper) {
        this.mapper = mapper;
        this.courseMapper = courseMapper;
        this.courseTemplateMapper = courseTemplateMapper;
    }

    public List<Student> list() {
        return mapper.listByUser(UserContext.getUserId());
    }

    public Student get(Long id) {
        Student s = mapper.findById(id, UserContext.getUserId());
        if (s == null) {
            throw new BusinessException(404, "学生不存在");
        }
        return s;
    }

    public Student create(StudentRequest request) {
        Student s = new Student();
        s.setUserId(UserContext.getUserId());
        apply(s, request);
        mapper.insert(s);
        return get(s.getId());
    }

    public Student update(Long id, StudentRequest request) {
        get(id);
        Student s = new Student();
        s.setId(id);
        s.setUserId(UserContext.getUserId());
        apply(s, request);
        mapper.update(s);
        return get(id);
    }

    public void delete(Long id) {
        get(id);
        Long userId = UserContext.getUserId();
        // 存在关联课程或课程模板时禁止删除，避免产生孤儿数据
        if (courseMapper.countByStudent(userId, id) > 0) {
            throw new BusinessException(400, "该学生名下还有课程，无法删除");
        }
        if (courseTemplateMapper.countByStudent(userId, id, null) > 0) {
            throw new BusinessException(400, "该学生已有课程模板，无法删除");
        }
        mapper.delete(id, userId);
    }

    private void apply(Student s, StudentRequest request) {
        s.setName(request.getName());
        s.setGender(request.getGender());
        s.setGrade(request.getGrade());
        s.setSubject(request.getSubject());
        s.setOrganizationId(request.getOrganizationId());
        s.setPhone(request.getPhone());
        s.setParentPhone(request.getParentPhone());
        s.setFee(request.getFee());
        s.setRemark(request.getRemark());
    }
}