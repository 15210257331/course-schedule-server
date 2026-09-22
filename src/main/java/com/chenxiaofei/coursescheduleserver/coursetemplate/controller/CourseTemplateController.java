package com.chenxiaofei.coursescheduleserver.coursetemplate.controller;

import com.chenxiaofei.coursescheduleserver.common.IdRequest;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplatePageRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.dto.CourseTemplateRequest;
import com.chenxiaofei.coursescheduleserver.coursetemplate.entity.CourseTemplate;
import com.chenxiaofei.coursescheduleserver.coursetemplate.service.CourseTemplateService;
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
@RequestMapping("/api/course-templates")
@RequiredArgsConstructor
public class CourseTemplateController {

    private final CourseTemplateService service;

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
    public Result<CourseTemplate> get(@RequestBody IdRequest body) {
        return Result.ok(service.get(body.getId()));
    }

    @PostMapping
    @OperationLog(module = "course_template", action = "CREATE", detail = "{#request.name}")
    public Result<CourseTemplate> create(@Valid @RequestBody CourseTemplateRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "course_template", action = "UPDATE")
    public Result<CourseTemplate> update(@PathVariable Long id, @Valid @RequestBody CourseTemplateRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "course_template", action = "DELETE")
    public Result<Integer> delete(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        boolean withCourses = body != null && Boolean.TRUE.equals(body.get("withCourses"));
        if (withCourses) {
            return Result.ok(service.deleteWithCourses(id));
        }
        service.delete(id);
        return Result.ok(0);
    }
}
