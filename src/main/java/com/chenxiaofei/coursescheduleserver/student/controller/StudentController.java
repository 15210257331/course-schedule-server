package com.chenxiaofei.coursescheduleserver.student.controller;

import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.student.dto.StudentRequest;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.student.service.StudentService;
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
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<Student>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/{id}")
    public Result<Student> get(@PathVariable Long id) {
        return Result.ok(service.get(id));
    }

    @PostMapping
    public Result<Student> create(@Valid @RequestBody StudentRequest request) {
        return Result.ok(service.create(request));
    }

    @PutMapping("/{id}")
    public Result<Student> update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return Result.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}