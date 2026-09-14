package com.chenxiaofei.coursescheduleserver.coursemessage.service;

import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import com.chenxiaofei.coursescheduleserver.coursemessage.entity.CourseMessage;
import com.chenxiaofei.coursescheduleserver.coursemessage.mapper.CourseMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 到点提醒生成：按课程的 reminder_offset_minutes（默认 30 分钟）在开课前的滑动窗口内，
 * 为 scheduled 课程生成一条提醒记录。每节课只生成一次，避免重复打扰。
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CourseMessageMapper courseMessageMapper;

    public ReminderScheduler(UserMapper userMapper, CourseMapper courseMapper, CourseMessageMapper courseMessageMapper) {
        this.userMapper = userMapper;
        this.courseMapper = courseMapper;
        this.courseMessageMapper = courseMessageMapper;
    }

    /** 每分钟一次：把刚进入提醒窗口的课程生成提醒 */
    @Scheduled(fixedDelay = 60_000)
    public void generateDueReminders() {
        try {
            List<Long> userIds = userMapper.listAllIds();
            if (userIds == null || userIds.isEmpty()) {
                return;
            }
            List<Course> due = courseMapper.listDueReminder(userIds, LocalDateTime.now());
            if (due == null || due.isEmpty()) {
                return;
            }
            int created = 0;
            for (Course c : due) {
                if (c.getUserId() == null || c.getId() == null) {
                    continue;
                }
                // 去重：每节课只提醒一次
                if (courseMessageMapper.countByCourse(c.getUserId(), c.getId()) > 0) {
                    continue;
                }
                CourseMessage n = new CourseMessage();
                n.setUserId(c.getUserId());
                n.setCourseId(c.getId());
                String name = c.getStudentName() != null && !c.getStudentName().isBlank()
                        ? c.getStudentName() : "学生";
                n.setTitle("课程即将开始");
                n.setContent(name + " · " + (c.getSubject() == null ? "" : c.getSubject())
                        + " " + c.getStartTime().format(HM) + " 开课");
                n.setRemindAt(LocalDateTime.now());
                courseMessageMapper.insert(n);
                created++;
            }
            if (created > 0) {
                log.info("已生成 {} 条到点提醒", created);
            }
        } catch (Exception e) {
            log.warn("到点提醒生成失败：{}", e.getMessage());
        }
    }
}
