package com.chenxiaofei.coursescheduleserver.operationlog.service;

import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Pages;
import com.chenxiaofei.coursescheduleserver.operationlog.dto.OperationLogPageRequest;
import com.chenxiaofei.coursescheduleserver.operationlog.entity.OperationLog;
import com.chenxiaofei.coursescheduleserver.operationlog.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 操作日志：记录与查询。
 */
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper mapper;

    /** 写入一条日志（由切面或业务代码调用） */
    public void record(OperationLog log) {
        mapper.insert(log);
    }

    /** 分页查询（管理端） */
    public PageResult<OperationLog> page(OperationLogPageRequest req) {
        return Pages.of(req,
                () -> mapper.count(req.getModule(), req.getAction(), req.getKeyword()),
                (offset, limit) -> mapper.page(req.getModule(), req.getAction(), req.getKeyword(), offset, limit));
    }
}
