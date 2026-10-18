package com.chenxiaofei.coursescheduleserver.upload.controller;

import com.chenxiaofei.coursescheduleserver.attachment.entity.Attachment;
import com.chenxiaofei.coursescheduleserver.attachment.service.AttachmentService;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.FileUploadValidator;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.config.PathResolver;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 头像上传：保存到本地目录，返回可访问的相对 URL（由 WebConfig 将 /uploads/** 映射到磁盘）。
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final AttachmentService attachmentService;
    private final PathResolver pathResolver;

    /** 课件上传：可关联多个学生（studentIds 逗号分隔或重复字段；兼容旧的单个 studentId） */
    @PostMapping("/attachment")
    public Result<Attachment> attachment(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "studentId", required = false) Long studentId,
                                         @RequestParam(value = "studentIds", required = false) List<String> studentIds) {
        List<Long> ids = new java.util.ArrayList<>();
        if (studentIds != null) {
            for (String s : studentIds) {
                if (s != null && !s.isBlank()) {
                    ids.add(Long.valueOf(s.trim()));
                }
            }
        }
        if (ids.isEmpty() && studentId != null) {
            ids.add(studentId);
        }
        return Result.ok(attachmentService.upload(ids, file));
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> avatar(@RequestParam("file") MultipartFile file) {
        String ext = FileUploadValidator.forAvatar().validate(file);
        try {
            Path dir = pathResolver.uploadDir().resolve("avatar");
            Files.createDirectories(dir);
            String name = "u" + UserContext.getUserId() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
            Path target = dir.resolve(name);
            file.transferTo(target.toFile());
            return Result.ok(Map.of("url", "/uploads/avatar/" + name));
        } catch (IOException e) {
            throw new BusinessException(500, "头像保存失败：" + e.getMessage());
        }
    }
}