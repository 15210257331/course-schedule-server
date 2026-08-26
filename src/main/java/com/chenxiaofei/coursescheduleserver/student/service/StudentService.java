package com.chenxiaofei.coursescheduleserver.student.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.student.dto.StudentRequest;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.student.mapper.StudentMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {

    private final StudentMapper mapper;

    public StudentService(StudentMapper mapper) {
        this.mapper = mapper;
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
        mapper.delete(id, UserContext.getUserId());
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