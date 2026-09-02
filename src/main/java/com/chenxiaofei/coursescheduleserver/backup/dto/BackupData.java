package com.chenxiaofei.coursescheduleserver.backup.dto;

import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.coursetemplate.entity.CourseTemplate;
import com.chenxiaofei.coursescheduleserver.notification.entity.Notification;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.setting.entity.Setting;
import lombok.Data;

import java.util.List;

/**
 * 备份载荷：当前用户全部业务数据快照。
 */
@Data
public class BackupData {

    private List<Organization> organizations;
    private List<CourseTemplate> templates;
    private List<Course> courses;
    private List<Notification> notifications;
    private List<Setting> settings;
}