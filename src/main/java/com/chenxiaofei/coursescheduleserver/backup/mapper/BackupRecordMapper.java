package com.chenxiaofei.coursescheduleserver.backup.mapper;

import com.chenxiaofei.coursescheduleserver.backup.entity.BackupRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BackupRecordMapper {

    int insert(BackupRecord record);

    List<BackupRecord> listRecent(@Param("limit") int limit);

    BackupRecord getById(@Param("id") Long id);
}
