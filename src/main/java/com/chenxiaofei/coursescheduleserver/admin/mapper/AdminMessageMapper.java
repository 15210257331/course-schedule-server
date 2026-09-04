package com.chenxiaofei.coursescheduleserver.admin.mapper;

import com.chenxiaofei.coursescheduleserver.admin.entity.AdminMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMessageMapper {

    int insert(AdminMessage message);

    int update(AdminMessage message);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int delete(@Param("id") Long id);

    AdminMessage findById(@Param("id") Long id);

    long countMessages(@Param("type") String type, @Param("status") String status, @Param("keyword") String keyword);

    List<AdminMessage> pageMessages(@Param("type") String type, @Param("status") String status,
                                    @Param("keyword") String keyword,
                                    @Param("offset") long offset, @Param("limit") int limit);

    /** 教师端：可见消息列表（all 或包含该教师的 specific，已发布） */
    List<AdminMessage> listForTeacher(@Param("userId") Long userId, @Param("limit") int limit);

    // ---- 阅读记录 ----

    int insertRead(@Param("messageId") Long messageId, @Param("userId") Long userId);

    int countRead(@Param("messageId") Long messageId);

    /** 当前用户已读的消息ID集合 */
    List<Long> listReadMessageIds(@Param("userId") Long userId);
}
