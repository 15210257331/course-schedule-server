package com.chenxiaofei.coursescheduleserver.upload.controller;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 头像上传：保存到本地目录，返回可访问的相对 URL（由 WebConfig 将 /uploads/** 映射到磁盘）。
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    private final Path uploadDir;

    public UploadController(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> avatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(400, "图片大小不能超过 2MB");
        }
        String original = file.getOriginalFilename();
        String ext = original == null ? "" : original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException(400, "仅支持 jpg/jpeg/png/gif/webp 图片");
        }
        try {
            Path dir = uploadDir.resolve("avatar");
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