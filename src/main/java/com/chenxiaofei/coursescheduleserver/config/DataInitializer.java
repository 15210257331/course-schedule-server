package com.chenxiaofei.coursescheduleserver.config;

import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.organization.mapper.OrganizationMapper;
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

    public DataInitializer(UserMapper userMapper, OrganizationMapper organizationMapper) {
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
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
}