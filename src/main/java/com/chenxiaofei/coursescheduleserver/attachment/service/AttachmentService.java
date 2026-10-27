package com.chenxiaofei.coursescheduleserver.attachment.service;

import com.chenxiaofei.coursescheduleserver.attachment.entity.Attachment;
import com.chenxiaofei.coursescheduleserver.attachment.mapper.AttachmentMapper;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.FileUploadValidator;
import com.chenxiaofei.coursescheduleserver.config.PathResolver;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import com.chenxiaofei.coursescheduleserver.student.entity.Student;
import com.chenxiaofei.coursescheduleserver.student.mapper.StudentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 课件：资源文件，通过 attachment_student 关联表挂到多个学生下。
 * 文件存 uploads/attachment/{userId}/ 下，按用户隔离。
 */
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentMapper mapper;
    private final StudentMapper studentMapper;
    private final PathResolver pathResolver;

    /** 某学生关联的课件列表 */
    public List<Attachment> list(Long studentId) {
        return mapper.listByStudent(UserContext.getUserId(), studentId);
    }

    public Attachment get(Long id) {
        Attachment a = mapper.findById(id, UserContext.getUserId());
        if (a == null) {
            throw new BusinessException(404, "课件不存在");
        }
        return a;
    }

    /** 上传课件并关联到多个学生 */
    @Transactional
    public Attachment upload(List<Long> studentIds, MultipartFile file) {
        Set<Long> ids = normalizeStudentIds(studentIds);
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
            a.setFileName(sanitizeFileName(file.getOriginalFilename()));
            a.setFilePath("/uploads/attachment/" + userId + "/" + stored);
            a.setFileSize(file.getSize());
            a.setMimeType(file.getContentType());
            mapper.insert(a);
            for (Long sid : ids) {
                mapper.insertStudentRef(userId, a.getId(), sid);
            }
            a.setStudentIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
            return a;
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败：" + e.getMessage());
        }
    }

    /** 修改课件关联的学生（全量覆盖） */
    @Transactional
    public Attachment updateStudents(Long id, List<Long> studentIds) {
        Attachment a = get(id);
        Set<Long> ids = normalizeStudentIds(studentIds);
        Long userId = UserContext.getUserId();
        mapper.deleteStudentRefsByAttachment(userId, a.getId());
        for (Long sid : ids) {
            mapper.insertStudentRef(userId, a.getId(), sid);
        }
        a.setStudentIds(ids.stream().map(String::valueOf).collect(Collectors.joining(",")));
        return a;
    }

    /** 解析磁盘绝对路径（供下载与删除） */
    public Path resolveFile(Attachment a) {
        String path = a.getFilePath();
        if (path == null || !path.startsWith("/uploads/")) {
            throw new BusinessException(500, "课件路径非法");
        }
        String relative = path.substring("/uploads/".length());
        return pathResolver.uploadDir().resolve(relative).normalize();
    }

    @Transactional
    public void delete(Long id) {
        Attachment a = get(id);
        deleteFile(a);
        mapper.deleteStudentRefsByAttachment(UserContext.getUserId(), id);
        mapper.delete(id, UserContext.getUserId());
    }

    /** 删除学生时级联清理其课件关联：不再关联任何学生的课件连同文件一并删除 */
    @Transactional
    public void deleteByStudent(Long studentId) {
        Long userId = UserContext.getUserId();
        // 先找出删完该学生关联后将变成孤儿的课件，再删关联，最后清孤儿
        List<Long> orphanIds = mapper.findOrphanAttachmentIds(userId, studentId);
        mapper.deleteStudentRefsByStudent(userId, studentId);
        for (Long id : orphanIds) {
            Attachment a = mapper.findById(id, userId);
            if (a != null) {
                deleteFile(a);
                mapper.delete(id, userId);
            }
        }
    }

    /** 校验并去重学生 id，全部不存在时抛错 */
    private Set<Long> normalizeStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            throw new BusinessException(400, "请至少选择一个关联学生");
        }
        Long userId = UserContext.getUserId();
        Set<Long> ids = new LinkedHashSet<>();
        for (Long sid : studentIds) {
            if (sid == null) {
                continue;
            }
            Student s = studentMapper.findById(sid, userId);
            if (s == null) {
                throw new BusinessException(400, "所选学生不存在或已删除");
            }
            ids.add(sid);
        }
        if (ids.isEmpty()) {
            throw new BusinessException(400, "请至少选择一个关联学生");
        }
        return ids;
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
