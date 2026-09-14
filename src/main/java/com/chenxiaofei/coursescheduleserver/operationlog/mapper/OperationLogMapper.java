package com.chenxiaofei.coursescheduleserver.operationlog.mapper;

import com.chenxiaofei.coursescheduleserver.operationlog.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    int insert(OperationLog log);

    long count(@Param("module") String module, @Param("action") String action, @Param("keyword") String keyword);

    List<OperationLog> page(@Param("module") String module, @Param("action") String action,
                            @Param("keyword") String keyword,
                            @Param("offset") long offset, @Param("limit") int limit);
}
