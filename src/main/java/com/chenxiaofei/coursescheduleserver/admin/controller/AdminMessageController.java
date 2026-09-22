package com.chenxiaofei.coursescheduleserver.admin.controller;

import com.chenxiaofei.coursescheduleserver.admin.dto.AdminMessagePageRequest;
import com.chenxiaofei.coursescheduleserver.admin.dto.AdminMessageRequest;
import com.chenxiaofei.coursescheduleserver.admin.entity.AdminMessage;
import com.chenxiaofei.coursescheduleserver.admin.service.AdminMessageService;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.common.IdRequest;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端 - 消息推送
 */
@RestController
@RequestMapping("/api/admin/messages")
@RequiredArgsConstructor
public class AdminMessageController {

    private final AdminMessageService service;

    @PostMapping("/page")
    public Result<PageResult<AdminMessage>> page(@RequestBody AdminMessagePageRequest request) {
        return Result.ok(service.page(request));
    }

    @PostMapping("/detail")
    public Result<AdminMessage> detail(@RequestBody IdRequest body) {
        return Result.ok(service.detail(body.getId()));
    }

    @PostMapping
    @OperationLog(module = "message", action = "CREATE")
    public Result<AdminMessage> create(@RequestBody AdminMessageRequest request) {
        return Result.ok(service.create(request));
    }

    @PostMapping("/update")
    @OperationLog(module = "message", action = "UPDATE")
    public Result<AdminMessage> update(@RequestBody AdminMessageRequest request) {
        return Result.ok(service.update(request.getId(), request));
    }

    @PostMapping("/revoke")
    @OperationLog(module = "message", action = "REVOKE")
    public Result<Void> revoke(@RequestBody IdRequest body) {
        service.revoke(body.getId());
        return Result.ok();
    }

    @PostMapping("/publish")
    @OperationLog(module = "message", action = "PUBLISH")
    public Result<Void> publish(@RequestBody IdRequest body) {
        service.publish(body.getId());
        return Result.ok();
    }

    @PostMapping("/delete")
    @OperationLog(module = "message", action = "DELETE")
    public Result<Void> delete(@RequestBody IdRequest body) {
        service.delete(body.getId());
        return Result.ok();
    }

    /** 教师选择器：简要教师列表 */
    @PostMapping("/teacher-options")
    public Result<List<User>> teacherOptions(@RequestBody(required = false) Map<String, String> body) {
        String keyword = body == null ? null : body.get("keyword");
        return Result.ok(service.teacherOptions(keyword));
    }
}
