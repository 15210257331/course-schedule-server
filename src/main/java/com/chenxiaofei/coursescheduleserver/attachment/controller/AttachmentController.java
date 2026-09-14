package com.chenxiaofei.coursescheduleserver.attachment.controller;

import com.chenxiaofei.coursescheduleserver.attachment.entity.Attachment;
import com.chenxiaofei.coursescheduleserver.attachment.service.AttachmentService;
import com.chenxiaofei.coursescheduleserver.common.Result;
import com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 附件：列表 / 下载 / 删除（上传见 UploadController.attachment）。
 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService service;

    public AttachmentController(AttachmentService service) {
        this.service = service;
    }

    /** 某课程模板下的附件列表 */
    @PostMapping("/list")
    public Result<List<Attachment>> list(@RequestBody Map<String, Object> body) {
        Object templateId = body.get("templateId");
        Long tid = templateId == null ? null : Long.valueOf(templateId.toString());
        return Result.ok(service.list(tid));
    }

    /** 下载：返回文件流，Content-Disposition 带原始文件名（UTF-8 编码支持中文名） */
    @PostMapping("/download")
    public ResponseEntity<Resource> download(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        Attachment a = service.get(id);
        Path file = service.resolveFile(a);
        if (!file.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }
        String encoded = URLEncoder.encode(a.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                .contentType(a.getMimeType() != null ? MediaType.parseMediaType(a.getMimeType())
                        : MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(a.getFileSize() != null ? a.getFileSize() : 0)
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{id}")
    @OperationLog(module = "attachment", action = "DELETE")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
