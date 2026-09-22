package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.admin.entity.AdminMessage;
import com.chenxiaofei.coursescheduleserver.admin.service.AdminMessageService;
import com.chenxiaofei.coursescheduleserver.common.IdRequest;
import com.chenxiaofei.coursescheduleserver.common.LimitRequest;
import com.chenxiaofei.coursescheduleserver.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 教师端 - 系统消息（公告/活动/通知）
 */
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final AdminMessageService service;

    @PostMapping("/list")
    public Result<List<AdminMessage>> list(@RequestBody(required = false) LimitRequest body) {
        int limit = (body != null && body.getLimit() != null) ? body.getLimit() : 20;
        return Result.ok(service.listForTeacher(limit));
    }

    @PostMapping("/read")
    public Result<Void> markRead(@RequestBody IdRequest body) {
        service.markRead(body.getId());
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        service.markAllRead();
        return Result.ok();
    }
}
