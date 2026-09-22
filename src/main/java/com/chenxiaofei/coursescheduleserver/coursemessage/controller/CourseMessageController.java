package com.chenxiaofei.coursescheduleserver.coursemessage.controller;

import com.chenxiaofei.coursescheduleserver.common.LimitRequest;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.coursemessage.entity.CourseMessage;
import com.chenxiaofei.coursescheduleserver.coursemessage.service.CourseMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/course-messages")
@RequiredArgsConstructor
public class CourseMessageController {

    private final CourseMessageService service;

    @PostMapping("/list")
    public Result<List<CourseMessage>> list(@RequestBody(required = false) LimitRequest body) {
        int limit = (body != null && body.getLimit() != null) ? body.getLimit() : 20;
        return Result.ok(service.list(limit));
    }

    @PostMapping("/due")
    public Result<List<CourseMessage>> due() {
        return Result.ok(service.due());
    }

    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        service.markRead(id);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        service.markAllRead();
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
