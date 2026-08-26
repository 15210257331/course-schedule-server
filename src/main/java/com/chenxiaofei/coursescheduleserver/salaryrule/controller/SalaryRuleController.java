package com.chenxiaofei.coursescheduleserver.salaryrule.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.salaryrule.dto.SalaryRuleRequest;
import com.chenxiaofei.coursescheduleserver.salaryrule.entity.SalaryRule;
import com.chenxiaofei.coursescheduleserver.salaryrule.service.SalaryRuleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/salary-rules")
public class SalaryRuleController {

    private final SalaryRuleService service;

    public SalaryRuleController(SalaryRuleService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<SalaryRule>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<SalaryRule> create(@Valid @RequestBody SalaryRuleRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public Result<SalaryRule> update(@PathVariable Long id, @Valid @RequestBody SalaryRuleRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}