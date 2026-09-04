package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.admin.entity.AdminMessage;
import com.chenxiaofei.coursescheduleserver.admin.service.AdminMessageService;
import com.chenxiaofei.coursescheduleserver.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 教师端 - 系统消息（公告/活动/通知）
 */
@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final AdminMessageService service;

    public MessageController(AdminMessageService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public Result<List<AdminMessage>> list(@RequestBody(required = false) Map<String, Integer> body) {
        int limit = (body != null && body.get("limit") != null) ? body.get("limit") : 20;
        return Result.ok(service.listForTeacher(limit));
    }

    @PostMapping("/read")
    public Result<Void> markRead(@RequestBody Map<String, Long> body) {
        service.markRead(body.get("id"));
        return Result.ok();
    }
}
