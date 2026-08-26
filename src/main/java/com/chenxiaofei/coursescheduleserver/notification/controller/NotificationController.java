package com.chenxiaofei.coursescheduleserver.notification.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.notification.entity.Notification;
import com.chenxiaofei.coursescheduleserver.notification.service.NotificationService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<Notification>> list(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(service.list(limit));
    }

    @GetMapping("/due")
    public Result<List<Notification>> due() {
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