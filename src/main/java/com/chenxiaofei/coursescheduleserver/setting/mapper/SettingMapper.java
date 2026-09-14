package com.chenxiaofei.coursescheduleserver.setting.mapper;

import com.chenxiaofei.coursescheduleserver.setting.entity.Setting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SettingMapper {

    List<Setting> listByUser(@Param("userId") Long userId);

    Setting get(@Param("userId") Long userId, @Param("key") String key);

    int upsert(@Param("userId") Long userId, @Param("key") String key, @Param("value") String value);
}
