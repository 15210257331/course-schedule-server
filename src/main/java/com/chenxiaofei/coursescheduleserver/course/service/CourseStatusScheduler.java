package com.chenxiaofei.coursescheduleserver.course.service;

import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.mapper.CourseMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 课程状态按时间自动结算：课程是否完成，统一以「当前时间 > 课程结束时间」判定。
 * 定时把 status 仍为 scheduled、但 end_time 已过期的课程置为 completed，
 * 从而让收入/课时等统计（口径统一按 end_time &lt; NOW()）与界面状态保持一致。
 */
@Component
public class CourseStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(CourseStatusScheduler.class);

    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    public CourseStatusScheduler(CourseMapper courseMapper, UserMapper userMapper) {
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
    }

    /** 每分钟跑一次：增量结算刚过结束时间的课程 */
    @Scheduled(fixedDelay = 60_000)
    public void settleExpiredCourses() {
        try {
            List<Long> userIds = userMapper.listAllIds();
            if (userIds == null || userIds.isEmpty()) {
                return;
            }
            List<Course> expired = courseMapper.listExpiredScheduled(userIds, LocalDateTime.now());
            if (expired == null || expired.isEmpty()) {
                return;
            }
            List<Long> ids = expired.stream().map(Course::getId).toList();
            int updated = courseMapper.batchUpdateStatus(ids, "completed");
            if (updated > 0) {
                log.info("课程自动结算：{} 节已过期置为 completed", updated);
            }
        } catch (Exception e) {
            log.warn("课程状态自动结算失败：{}", e.getMessage());
        }
    }
}