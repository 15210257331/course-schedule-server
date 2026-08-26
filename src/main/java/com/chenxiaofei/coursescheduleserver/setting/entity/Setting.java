package com.chenxiaofei.coursescheduleserver.setting.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Setting {

    private Long id;
    private Long userId;
    private String settingKey;
    private String settingValue;
    private LocalDateTime updatedAt;
}