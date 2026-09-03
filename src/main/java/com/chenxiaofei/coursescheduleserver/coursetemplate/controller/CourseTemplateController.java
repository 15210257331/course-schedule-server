package com.chenxiaofei.coursescheduleserver.coursetemplate.controller;

import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplatePageRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplateRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.entity.CourseTemplate;
import com.chenxiaofei.coursescheduleserver.coursetemplate.service.CourseTemplateService;
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
@RequestMapping("/api/course-templates")
public class CourseTemplateController {

    private final CourseTemplateService service;

    public CourseTemplateController(CourseTemplateService service) {
        this.service = service;
    }

    @PostMapping("/list")
    public Result<List<CourseTemplate>> list(@RequestBody(required = false) Map<String, String> body) {
        String name = (body != null) ? body.get("name") : null;
        String kw = (name == null || name.isBlank()) ? null : name.trim();
        return Result.ok(service.list(kw));
    }

    @PostMapping("/page")
    public Result<PageResult<CourseTemplate>> page(@RequestBody CourseTemplatePageRequest request) {
        return Result.ok(service.page(request));
    }

    @PostMapping("/detail")
    public Result<CourseTemplate> get(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        return Result.ok(service.get(id));
    }

    @PostMapping
    public Result<CourseTemplate> create(@Valid @RequestBody CourseTemplateRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public Result<CourseTemplate> update(@PathVariable Long id, @Valid @RequestBody CourseTemplateRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
