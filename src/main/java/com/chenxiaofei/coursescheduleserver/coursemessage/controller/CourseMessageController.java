package com.chenxiaofei.coursescheduleserver.coursemessage.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.coursemessage.entity.CourseMessage;
import com.chenxiaofei.coursescheduleserver.coursemessage.service.CourseMessageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/course-messages")
public class CourseMessageController {

    private final CourseMessageService service;

    public CourseMessageController(CourseMessageService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public Result<List<CourseMessage>> list(@RequestBody(required = false) Map<String, Integer> body) {
        int limit = (body != null && body.get("limit") != null) ? body.get("limit") : 20;
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
