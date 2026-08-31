package com.chenxiaofei.coursescheduleserver.config;

import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.organization.mapper.OrganizationMapper;
import com.chenxiaofei.coursescheduleserver.student.mapper.StudentMapper;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 首次启动时初始化默认账号与演示数据（仅在 user 表为空时执行）
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final StudentMapper studentMapper;

    public DataInitializer(UserMapper userMapper, OrganizationMapper organizationMapper,
                           StudentMapper studentMapper) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
        this.studentMapper = studentMapper;
    }

    @Override
    public void run(String... args) {
        if (userMapper.findByUsername("admin") != null) {
            return;
        }
        log.info("检测到首次启动，正在初始化默认账号 admin / admin123 ...");

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
        admin.setNickname("晓飞老师");
        admin.setRole("ADMIN");
        admin.setEmail("admin@teacheros.local");
        userMapper.insert(admin);
        Long uid = admin.getId();

        Organization xdf = org(uid, "新东方", "王老师", "13800000001", "北京市海淀区中关村大街 1 号", "300");
        Organization xes = org(uid, "学而思", "李老师", "13800000002", "北京市朝阳区望京 SOHO", "320");
        organizationMapper.insert(xdf);
        organizationMapper.insert(xes);

        studentMapper.insert(stu(uid, "张三", "初三", "数学", xdf.getId(), "350"));
        studentMapper.insert(stu(uid, "李四", "高一", "物理", xdf.getId(), "380"));
        studentMapper.insert(stu(uid, "王五", "初二", "英语", xes.getId(), "300"));
        studentMapper.insert(stu(uid, "赵六", "高三", "数学", xes.getId(), "400"));

        log.info("初始化完成。默认账号：admin / admin123");
    }

    private Organization org(Long uid, String name, String contact, String phone, String address, String fee) {
        Organization o = new Organization();
        o.setUserId(uid);
        o.setName(name);
        o.setContactName(contact);
        o.setContactPhone(phone);
        o.setAddress(address);
        o.setDefaultFee(new BigDecimal(fee));
        return o;
    }

    private Student stu(Long uid, String name, String grade, String subject, Long orgId, String fee) {
        Student s = new Student();
        s.setUserId(uid);
        s.setName(name);
        s.setGrade(grade);
        s.setSubject(subject);
        s.setOrganizationId(orgId);
        s.setFee(new BigDecimal(fee));
        return s;
    }
}