package com.chenxiaofei.coursescheduleserver.organization.controller;

import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationPageRequest;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationRequest;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.organization.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public Result<List<Organization>> list(@RequestBody(required = false) Map<String, String> body) {
        String name = (body != null) ? body.get("name") : null;
        String kw = (name == null || name.isBlank()) ? null : name.trim();
        return Result.ok(service.list(kw));
    }

    @PostMapping("/page")
    public Result<PageResult<Organization>> page(@RequestBody OrganizationPageRequest request) {
        return Result.ok(service.page(request));
    }

    @PostMapping("/detail")
    public Result<Organization> get(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        return Result.ok(service.get(id));
    }

    @PostMapping
    public Result<Organization> create(@Valid @RequestBody OrganizationRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public Result<Organization> update(@PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}