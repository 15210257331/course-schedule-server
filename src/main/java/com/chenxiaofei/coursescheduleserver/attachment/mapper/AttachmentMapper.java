package com.chenxiaofei.coursescheduleserver.attachment.mapper;

import com.chenxiaofei.coursescheduleserver.attachment.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    /** 某课程模板下的全部附件 */
    List<Attachment> listByTemplate(@Param("userId") Long userId, @Param("templateId") Long templateId);

    /** 查询当前用户全部附件（迁移/审计用） */
    List<Attachment> listByUser(@Param("userId") Long userId);

    Attachment findById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Attachment a);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    /** 删除某课程模板下的全部附件（模板删除级联用） */
    int deleteByTemplate(@Param("userId") Long userId, @Param("templateId") Long templateId);
}
