package com.chenxiaofei.coursescheduleserver.attachmentgroup.mapper;

import com.chenxiaofei.coursescheduleserver.attachmentgroup.entity.AttachmentGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachmentGroupMapper {

    List<AttachmentGroup> listByUser(@Param("userId") Long userId);

    AttachmentGroup findById(@Param("id") Long id, @Param("userId") Long userId);

    int countByName(@Param("userId") Long userId, @Param("name") String name, @Param("excludeId") Long excludeId);

    int insert(AttachmentGroup group);

    int update(AttachmentGroup group);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    int deleteAllByUser(@Param("userId") Long userId);
}