package com.chenxiaofei.coursescheduleserver.attachmentgroup.controller;

import com.chenxiaofei.coursescheduleserver.attachmentgroup.dto.AttachmentGroupRequest;
import com.chenxiaofei.coursescheduleserver.attachmentgroup.entity.AttachmentGroup;
import com.chenxiaofei.coursescheduleserver.attachmentgroup.service.AttachmentGroupService;
import com.chenxiaofei.coursescheduleserver.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 附件分组：增删改查接口。
 */
@RestController
@RequestMapping("/api/attachment-groups")
public class AttachmentGroupController {

    private final AttachmentGroupService service;

    public AttachmentGroupController(AttachmentGroupService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public Result<List<AttachmentGroup>> list() {
        return Result.ok(service.list());
    }

    @PostMapping
    public Result<AttachmentGroup> create(@Valid @RequestBody AttachmentGroupRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public Result<AttachmentGroup> update(@PathVariable Long id, @Valid @RequestBody AttachmentGroupRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}