package com.chenxiaofei.coursescheduleserver.attachment.mapper;

import com.chenxiaofei.coursescheduleserver.attachment.entity.Attachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttachmentMapper {

    List<Attachment> listByBiz(@Param("userId") Long userId, @Param("bizType") String bizType, @Param("bizId") Long bizId);

    /** 查询当前用户全部附件（附件管理页面用，不再局限于单个业务对象） */
    List<Attachment> listByUser(@Param("userId") Long userId);

    /** 更新附件归属分组（附件管理页「移动到分组」） */
    int updateGroup(@Param("id") Long id, @Param("userId") Long userId, @Param("groupId") Long groupId);

    /** 删除某分组下的全部附件（分组删除级联用） */
    int deleteByGroup(@Param("userId") Long userId, @Param("groupId") Long groupId);

    Attachment findById(@Param("id") Long id, @Param("userId") Long userId);

    int insert(Attachment a);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    /** 删除某业务对象下的全部附件（模板删除级联用） */
    int deleteByBiz(@Param("userId") Long userId, @Param("bizType") String bizType, @Param("bizId") Long bizId);

    /** 删除当前用户全部附件（数据导入前清空用） */
    int deleteAllByUser(@Param("userId") Long userId);
}
