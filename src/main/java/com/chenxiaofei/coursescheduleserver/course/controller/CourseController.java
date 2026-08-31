package com.chenxiaofei.coursescheduleserver.course.controller;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.course.dto.CoursePageRequest;
import com.chenxiaofei.coursescheduleserver.course.dto.CourseRequest;
import com.chenxiaofei.coursescheduleserver.course.dto.MoveCourseRequest;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Result<List<Course>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(required = false) String title) {
        if (start == null || end == null) {
            throw new BusinessException(400, "缺少 start/end 时间范围参数");
        }
        String kw = (title == null || title.isBlank()) ? null : title.trim();
        return Result.ok(courseService.listInRange(start, end, kw));
    }

    @PostMapping("/page")
    public Result<PageResult<Course>> page(@RequestBody CoursePageRequest request) {
        return Result.ok(courseService.page(request));
    }

    @GetMapping("/{id}")
    public Result<Course> get(@PathVariable Long id) {
        return Result.ok(courseService.get(id));
    }

    @PostMapping
    public Result<Course> create(@Valid @RequestBody CourseRequest request) {
        return Result.ok(courseService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Course> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return Result.ok(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/copy")
    public Result<Course> copy(@PathVariable Long id, @RequestBody MoveCourseRequest request) {
        return Result.ok(courseService.copy(id, request.getStartTime(), request.getEndTime()));
    }

    @PutMapping("/{id}/move")
    public Result<Course> move(@PathVariable Long id, @RequestBody MoveCourseRequest request) {
        return Result.ok(courseService.move(id, request.getStartTime(), request.getEndTime()));
    }

    @PostMapping("/copy-week")
    public Result<Integer> copyWeek(@RequestParam(defaultValue = "1") int week) {
        return Result.ok(courseService.copyWeekTo(week));
    }
}