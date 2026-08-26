package com.chenxiaofei.coursescheduleserver.organization.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.organization.dto.OrganizationRequest;
import com.chenxiaofei.coursescheduleserver.organization.entity.Organization;
import com.chenxiaofei.coursescheduleserver.organization.service.OrganizationService;
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
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<Organization>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/{id}")
    public Result<Organization> get(@PathVariable Long id) {
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