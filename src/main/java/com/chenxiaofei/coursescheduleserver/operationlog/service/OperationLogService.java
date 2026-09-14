package com.chenxiaofei.coursescheduleserver.operationlog.service;

import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.operationlog.dto.OperationLogPageRequest;
import com.chenxiaofei.coursescheduleserver.operationlog.entity.OperationLog;
import com.chenxiaofei.coursescheduleserver.operationlog.mapper.OperationLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 操作日志：记录与查询。
 */
@Service
public class OperationLogService {

    private final OperationLogMapper mapper;

    public OperationLogService(OperationLogMapper mapper) {
        this.mapper = mapper;
    }

    /** 写入一条日志（由切面或业务代码调用） */
    public void record(OperationLog log) {
        mapper.insert(log);
    }

    /** 分页查询（管理端） */
    public PageResult<OperationLog> page(OperationLogPageRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 20 : req.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;
        long total = mapper.count(req.getModule(), req.getAction(), req.getKeyword());
        List<OperationLog> list = mapper.page(req.getModule(), req.getAction(), req.getKeyword(), offset, pageSize);
        return PageResult.of(total, list);
    }
}
