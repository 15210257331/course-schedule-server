package com.chenxiaofei.coursescheduleserver.notification.service;

import com.chenxiaofei.coursescheduleserver.notification.entity.Notification;
import com.chenxiaofei.coursescheduleserver.notification.mapper.NotificationMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationMapper mapper;

    public NotificationService(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    public List<Notification> list(int limit) {
        int l = Math.min(Math.max(limit, 1), 100);
        return mapper.listRecent(UserContext.getUserId(), l);
    }

    public List<Notification> due() {
        return mapper.listDue(UserContext.getUserId(), LocalDateTime.now());
    }

    public void markRead(Long id) {
        mapper.markRead(id, UserContext.getUserId());
    }

    public void markAllRead() {
        mapper.markAllRead(UserContext.getUserId());
    }

    public void delete(Long id) {
        mapper.delete(id, UserContext.getUserId());
    }
}