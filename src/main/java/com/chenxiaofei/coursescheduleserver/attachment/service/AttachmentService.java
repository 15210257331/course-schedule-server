package com.chenxiaofei.coursescheduleserver.attachment.service;

import com.chenxiaofei.coursescheduleserver.attachment.entity.Attachment;
import com.chenxiaofei.coursescheduleserver.attachment.mapper.AttachmentMapper;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.FileUploadValidator;
import com.chenxiaofei.coursescheduleserver.config.PathResolver;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 附件：挂在课程模板（学生）下的资源文件。文件存 uploads/attachment/{userId}/ 下，按用户隔离。
 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentMapper mapper;
    private final PathResolver pathResolver;

    /** 某课程模板下的附件列表 */
    public List<Attachment> list(Long templateId) {
        return mapper.listByTemplate(UserContext.getUserId(), templateId);
    }

    public Attachment get(Long id) {
        Attachment a = mapper.findById(id, UserContext.getUserId());
        if (a == null) {
            throw new BusinessException(404, "附件不存在");
        }
        return a;
    }

    @Transactional
    public Attachment upload(Long templateId, MultipartFile file) {
        String ext = FileUploadValidator.forAttachment().validate(file);
        Long userId = UserContext.getUserId();
        try {
            Path dir = pathResolver.uploadDir().resolve("attachment").resolve(String.valueOf(userId));
            Files.createDirectories(dir);
            String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = dir.resolve(stored);
            file.transferTo(target.toFile());

            Attachment a = new Attachment();
            a.setUserId(userId);
            a.setTemplateId(templateId);
            a.setFileName(sanitizeFileName(file.getOriginalFilename()));
            a.setFilePath("/uploads/attachment/" + userId + "/" + stored);
            a.setFileSize(file.getSize());
            a.setMimeType(file.getContentType());
            mapper.insert(a);
            return a;
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败：" + e.getMessage());
        }
    }

    /** 解析磁盘绝对路径（供下载与删除） */
    public Path resolveFile(Attachment a) {
        String path = a.getFilePath();
        if (path == null || !path.startsWith("/uploads/")) {
            throw new BusinessException(500, "附件路径非法");
        }
        String relative = path.substring("/uploads/".length());
        return pathResolver.uploadDir().resolve(relative).normalize();
    }

    @Transactional
    public void delete(Long id) {
        Attachment a = get(id);
        deleteFile(a);
        mapper.delete(id, UserContext.getUserId());
    }

    /** 删除课程模板时级联清理其全部附件（含磁盘文件） */
    @Transactional
    public void deleteByTemplate(Long templateId) {
        Long userId = UserContext.getUserId();
        for (Attachment a : mapper.listByTemplate(userId, templateId)) {
            deleteFile(a);
        }
        mapper.deleteByTemplate(userId, templateId);
    }

    private void deleteFile(Attachment a) {
        try {
            Files.deleteIfExists(resolveFile(a));
        } catch (IOException ignored) {
            // 文件缺失不影响删除记录
        }
    }

    /** 原文件名仅作展示，去掉路径分隔符与危险字符 */
    private String sanitizeFileName(String name) {
        if (name == null) {
            return "file";
        }
        String base = Paths.get(name).getFileName().toString();
        return base.isBlank() ? "file" : base;
    }
}
