package com.chenxiaofei.coursescheduleserver.organization.controller;

import com.chenxiaofei.coursescheduleserver.common.IdRequest;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationPageRequest;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationRequest;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.organization.service.OrganizationService;
import com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService service;

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
    public Result<Organization> get(@RequestBody IdRequest body) {
        return Result.ok(service.get(body.getId()));
    }

    @PostMapping
    @OperationLog(module = "organization", action = "CREATE", detail = "{#request.name}")
    public Result<Organization> create(@Valid @RequestBody OrganizationRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "organization", action = "UPDATE")
    public Result<Organization> update(@PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "organization", action = "DELETE")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}