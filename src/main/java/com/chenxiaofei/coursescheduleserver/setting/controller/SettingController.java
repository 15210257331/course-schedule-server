package com.chenxiaofei.coursescheduleserver.setting.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.setting.dto.SettingsSaveRequest;
import com.chenxiaofei.coursescheduleserver.setting.service.SettingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
public class SettingController {

    private final SettingService service;

    public SettingController(SettingService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public Result<Map<String, String>> list() {
        return Result.ok(service.list());
    }

    @PutMapping
    public Result<Map<String, String>> save(@RequestBody SettingsSaveRequest request) {
        return Result.ok(service.save(request.getSettings()));
    }
}