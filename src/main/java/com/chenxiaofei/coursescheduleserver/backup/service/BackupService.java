package com.chenxiaofei.coursescheduleserver.backup.service;

import com.chenxiaofei.coursescheduleserver.backup.dto.BackupData;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.coursetemplate.entity.CourseTemplate;
import com.chenxiaofei.coursescheduleserver.coursetemplate.mapper.CourseTemplateMapper;
import com.chenxiaofei.coursescheduleserver.notification.entity.Notification;
import com.chenxiaofei.coursescheduleserver.notification.mapper.NotificationMapper;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.organization.mapper.OrganizationMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.setting.entity.Setting;
import com.chenxiaofei.coursescheduleserver.setting.mapper.SettingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据备份：导出当前用户全部业务数据为 JSON，导入则先清空再回填（全量替换）。
 * 导入时保留原 id、created_at 等主键，并对机构 / 课程 / 模板做 id 重映射，
 * 以正确还原 organization_id 与课程的父子重复系列关系。
 */
@Service
public class BackupService {

    private final OrganizationMapper organizationMapper;
    private final CourseTemplateMapper templateMapper;
    private final CourseMapper courseMapper;
    private final NotificationMapper notificationMapper;
    private final SettingMapper settingMapper;

    public BackupService(OrganizationMapper organizationMapper, CourseTemplateMapper templateMapper,
                         CourseMapper courseMapper, NotificationMapper notificationMapper,
                         SettingMapper settingMapper) {
        this.organizationMapper = organizationMapper;
        this.templateMapper = templateMapper;
        this.courseMapper = courseMapper;
        this.notificationMapper = notificationMapper;
        this.settingMapper = settingMapper;
    }

    public BackupData exportData() {
        Long userId = UserContext.getUserId();
        BackupData data = new BackupData();
        data.setOrganizations(organizationMapper.listAllByUser(userId));
        data.setTemplates(templateMapper.listAllByUser(userId));
        data.setCourses(courseMapper.listAllByUser(userId));
        data.setNotifications(notificationMapper.listAllByUser(userId));
        data.setSettings(settingMapper.listByUser(userId));
        return data;
    }

    /** 导入：清空当前用户数据后按备份内容重建（不覆盖用户账号与密码） */
    @Transactional
    public int importData(BackupData data) {
        if (data == null) {
            throw new BusinessException(400, "备份内容为空");
        }
        Long userId = UserContext.getUserId();
        courseMapper.deleteAllByUser(userId);
        templateMapper.deleteAllByUser(userId);
        organizationMapper.deleteAllByUser(userId);
        notificationMapper.deleteAllByUser(userId);
        settingMapper.deleteAllByUser(userId);

        // 机构 id 重映射：导入数据里的 org 主键可能与当前库冲突
        Map<Long, Long> orgIdMap = new LinkedHashMap<>();
        for (Organization org : nvl(data.getOrganizations())) {
            Long oldId = org.getId();
            org.setId(null);
            org.setUserId(userId);
            organizationMapper.insert(org);
            if (oldId != null) {
                orgIdMap.put(oldId, org.getId());
            }
        }

        Map<Long, Long> tplIdMap = new LinkedHashMap<>();
        for (CourseTemplate t : nvl(data.getTemplates())) {
            Long oldId = t.getId();
            t.setId(null);
            t.setUserId(userId);
            if (t.getOrganizationId() != null) {
                t.setOrganizationId(orgIdMap.getOrDefault(t.getOrganizationId(), t.getOrganizationId()));
            }
            t.setStudentId(null);
            templateMapper.insert(t);
            if (oldId != null) {
                tplIdMap.put(oldId, t.getId());
            }
        }

        Map<Long, Long> courseIdMap = new LinkedHashMap<>();
        for (Course c : nvl(data.getCourses())) {
            Long oldId = c.getId();
            c.setId(null);
            c.setUserId(userId);
            if (c.getOrganizationId() != null) {
                c.setOrganizationId(orgIdMap.getOrDefault(c.getOrganizationId(), c.getOrganizationId()));
            }
            c.setStudentId(null);
            courseMapper.insert(c);
            if (oldId != null) {
                courseIdMap.put(oldId, c.getId());
            }
        }
        // 回填课程重复系列父子关系（parent_id 引用旧 id）
        for (Course c : nvl(data.getCourses())) {
            if (c.getParentId() != null && courseIdMap.containsKey(c.getParentId())) {
                courseMapper.updateParent(courseIdMap.get(c.getId()), userId, courseIdMap.get(c.getParentId()));
            }
        }

        for (Notification n : nvl(data.getNotifications())) {
            n.setId(null);
            n.setUserId(userId);
            if (n.getCourseId() != null) {
                n.setCourseId(courseIdMap.getOrDefault(n.getCourseId(), n.getCourseId()));
            }
            notificationMapper.insertFull(n);
        }

        for (Setting s : nvl(data.getSettings())) {
            settingMapper.upsert(userId, s.getSettingKey(), s.getSettingValue());
        }
        return data.getCourses() == null ? 0 : data.getCourses().size();
    }

    private static <T> List<T> nvl(List<T> list) {
        return list == null ? List.of() : list;
    }
}