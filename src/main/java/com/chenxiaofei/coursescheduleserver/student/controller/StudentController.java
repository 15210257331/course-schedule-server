package com.chenxiaofei.coursescheduleserver.student.controller;

import com.chenxiaofei.coursescheduleserver.common.IdRequest;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.student.dto.StudentPageRequest;
import com.chenxiaofei.coursescheduleserver.student.dto.StudentRequest;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.student.service.StudentService;
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
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    @PostMapping("/list")
    public Result<List<Student>> list(@RequestBody(required = false) Map<String, String> body) {
        String name = (body != null) ? body.get("name") : null;
        String kw = (name == null || name.isBlank()) ? null : name.trim();
        return Result.ok(service.list(kw));
    }

    @PostMapping("/page")
    public Result<PageResult<Student>> page(@RequestBody StudentPageRequest request) {
        return Result.ok(service.page(request));
    }

    @PostMapping("/detail")
    public Result<Student> get(@RequestBody IdRequest body) {
        return Result.ok(service.get(body.getId()));
    }

    @PostMapping
    @OperationLog(module = "student", action = "CREATE", detail = "{#request.name}")
    public Result<Student> create(@Valid @RequestBody StudentRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "student", action = "UPDATE")
    public Result<Student> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "student", action = "DELETE")
    public Result<Integer> delete(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        boolean withCourses = body != null && Boolean.TRUE.equals(body.get("withCourses"));
        if (withCourses) {
            return Result.ok(service.deleteWithCourses(id));
        }
        service.delete(id);
        return Result.ok(0);
    }
}
