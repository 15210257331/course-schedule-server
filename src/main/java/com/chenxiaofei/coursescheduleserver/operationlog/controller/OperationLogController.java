package com.chenxiaofei.coursescheduleserver.operationlog.controller;

import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.operationlog.dto.OperationLogPageRequest;
import com.chenxiaofei.coursescheduleserver.operationlog.entity.OperationLog;
import com.chenxiaofei.coursescheduleserver.operationlog.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端 - 操作日志
 */
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final OperationLogService service;

    @PostMapping("/page")
    public Result<PageResult<OperationLog>> page(@RequestBody OperationLogPageRequest request) {
        return Result.ok(service.page(request));
    }
}
