package com.chenxiaofei.coursescheduleserver.setting.dto;

import lombok.Data;

import java.util.Map;

@Data
public class SettingsSaveRequest {

    private Map<String, String> settings;
}