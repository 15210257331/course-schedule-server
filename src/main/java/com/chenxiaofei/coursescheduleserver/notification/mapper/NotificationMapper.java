package com.chenxiaofei.coursescheduleserver.notification.mapper;

import com.chenxiaofei.coursescheduleserver.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {

    List<Notification> listDue(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    List<Notification> listRecent(@Param("userId") Long userId, @Param("limit") int limit);

    int insert(Notification n);

    int markRead(@Param("id") Long id, @Param("userId") Long userId);

    int markAllRead(@Param("userId") Long userId);

    int delete(@Param("id") Long id, @Param("userId") Long userId);
}
