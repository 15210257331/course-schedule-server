package com.chenxiaofei.coursescheduleserver.setting.service;

import com.chenxiaofei.coursescheduleserver.setting.entity.Setting;
import com.chenxiaofei.coursescheduleserver.setting.mapper.SettingMapper;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SettingService {

    private final SettingMapper mapper;

    public Map<String, String> list() {
        List<Setting> settings = mapper.listByUser(UserContext.getUserId());
        Map<String, String> result = new LinkedHashMap<>();
        for (Setting s : settings) {
            result.put(s.getSettingKey(), s.getSettingValue());
        }
        return result;
    }

    public Map<String, String> save(Map<String, String> settings) {
        Long userId = UserContext.getUserId();
        for (Map.Entry<String, String> e : settings.entrySet()) {
            mapper.upsert(userId, e.getKey(), e.getValue());
        }
        return list();
    }
}