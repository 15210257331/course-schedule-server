package com.chenxiaofei.coursescheduleserver.course.controller;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.course.dto.CoursePageRequest;
import com.chenxiaofei.coursescheduleserver.course.dto.CopyWeekResult;
import com.chenxiaofei.coursescheduleserver.course.dto.CourseRequest;
import com.chenxiaofei.coursescheduleserver.course.dto.MoveCourseRequest;
import com.chenxiaofei.coursescheduleserver.course.entity.Course;
import com.chenxiaofei.coursescheduleserver.course.service.CourseService;
import com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/list")
    public Result<List<Course>> list(@RequestBody Map<String, String> body) {
        String startStr = body.get("start");
        String endStr = body.get("end");
        String title = body.get("title");
        if (startStr == null || endStr == null) {
            throw new BusinessException(400, "缺少 start/end 时间范围参数");
        }
        LocalDateTime start = LocalDateTime.parse(startStr);
        LocalDateTime end = LocalDateTime.parse(endStr);
        String kw = (title == null || title.isBlank()) ? null : title.trim();
        return Result.ok(courseService.listInRange(start, end, kw));
    }

    @PostMapping("/page")
    public Result<PageResult<Course>> page(@RequestBody CoursePageRequest request) {
        return Result.ok(courseService.page(request));
    }

    @PostMapping("/detail")
    public Result<Course> get(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        return Result.ok(courseService.get(id));
    }

    @PostMapping
    @OperationLog(module = "course", action = "CREATE", detail = "{#request.title}")
    public Result<Course> create(@Valid @RequestBody CourseRequest request) {
        return Result.ok(courseService.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog(module = "course", action = "UPDATE")
    public Result<Course> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return Result.ok(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "course", action = "DELETE")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.ok();
    }

    @PutMapping("/{id}/move")
    @OperationLog(module = "course", action = "MOVE")
    public Result<Course> move(@PathVariable Long id, @RequestBody MoveCourseRequest request) {
        return Result.ok(courseService.move(id, request.getStartTime(), request.getEndTime()));
    }

    @PostMapping("/copy-week")
    @OperationLog(module = "course", action = "COPY_WEEK")
    public Result<CopyWeekResult> copyWeek(@RequestBody Map<String, Object> body) {
        Object src = body.get("sourceMonday");
        String sourceMonday = src == null ? null : String.valueOf(src);
        int week = body.get("week") == null ? 1 : Integer.parseInt(String.valueOf(body.get("week")));
        return Result.ok(courseService.copyWeekTo(sourceMonday, week));
    }
}