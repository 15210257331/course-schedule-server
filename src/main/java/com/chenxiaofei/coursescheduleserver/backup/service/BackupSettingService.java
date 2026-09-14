package com.chenxiaofei.coursescheduleserver.backup.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.config.BackupProperties;
import com.chenxiaofei.coursescheduleserver.setting.entity.Setting;
import com.chenxiaofei.coursescheduleserver.setting.mapper.SettingMapper;
import org.springframework.stereotype.Service;

/**
 * 自动备份全局开关（存数据库，管理端可改）：
 * 控制备份文件落到「服务器本地」还是「腾讯云 COS」。
 * 存储于 setting 表，user_id = 0 表示系统级配置（区别于每个教师各自的个人设置）。
 * 无人工设置时回退到 application.yaml 的 app.backup.cos.enabled 作为默认值。
 */
@Service
public class BackupSettingService {

    private static final String KEY_STORAGE_TYPE = "backup.storageType";
    private static final long SYSTEM_USER_ID = 0L;

    private final SettingMapper settingMapper;
    private final BackupProperties properties;

    public BackupSettingService(SettingMapper settingMapper, BackupProperties properties) {
        this.settingMapper = settingMapper;
        this.properties = properties;
    }

    /** 当前全局存储位置：cos / local */
    public String storageType() {
        Setting s = settingMapper.get(SYSTEM_USER_ID, KEY_STORAGE_TYPE);
        if (s != null && "cos".equals(s.getSettingValue())) {
            return "cos";
        }
        if (s != null && "local".equals(s.getSettingValue())) {
            return "local";
        }
        return properties.getCos().isEnabled() ? "cos" : "local";
    }

    public boolean useCos() {
        return "cos".equals(storageType());
    }

    /** 更新全局存储位置（仅管理端调用） */
    public void setStorageType(String storageType) {
        if (!"cos".equals(storageType) && !"local".equals(storageType)) {
            throw new BusinessException(400, "存储位置仅支持 cos / local");
        }
        settingMapper.upsert(SYSTEM_USER_ID, KEY_STORAGE_TYPE, storageType);
    }
}
