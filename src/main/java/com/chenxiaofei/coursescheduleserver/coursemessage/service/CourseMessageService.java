package com.chenxiaofei.coursescheduleserver.coursemessage.service;

import com.chenxiaofei.coursescheduleserver.coursemessage.entity.CourseMessage;
import com.chenxiaofei.coursescheduleserver.coursemessage.mapper.CourseMessageMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseMessageService {

    private final CourseMessageMapper mapper;

    public List<CourseMessage> list(int limit) {
        int l = Math.min(Math.max(limit, 1), 100);
        return mapper.listRecent(UserContext.getUserId(), l);
    }

    public List<CourseMessage> due() {
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
