package com.chenxiaofei.coursescheduleserver.common;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 文件上传校验：集中「空判 + 大小 + 扩展名」逻辑，消除 AttachmentService / UploadController 的重复校验块。
 * 校验通过返回小写扩展名（无扩展名返回空串），供调用方生成存储文件名。
 */
public final class FileUploadValidator {

    private final Set<String> allowedExt;
    private final long maxSize;
    private final String label;

    private FileUploadValidator(Set<String> allowedExt, long maxSize, String label) {
        this.allowedExt = allowedExt;
        this.maxSize = maxSize;
        this.label = label;
    }

    /** 附件上传：14 类常见文档/图片，上限 20MB */
    public static FileUploadValidator forAttachment() {
        return new FileUploadValidator(Set.of(
                "jpg", "jpeg", "png", "gif", "webp",
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip"),
                20L * 1024 * 1024, "文件");
    }

    /** 头像上传：仅图片，上限 2MB */
    public static FileUploadValidator forAvatar() {
        return new FileUploadValidator(Set.of("jpg", "jpeg", "png", "gif", "webp"),
                2L * 1024 * 1024, "图片");
    }

    /**
     * 校验上传文件：空判 → 大小 → 扩展名。通过返回小写扩展名（无扩展名为空串）。
     * 无点文件名不再越界（原 UploadController 的潜在 bug）。
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请选择要上传的" + label);
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(400, label + "大小不能超过 " + (maxSize >> 20) + "MB");
        }
        String original = file.getOriginalFilename();
        String ext = (original == null || !original.contains("."))
                ? "" : original.substring(original.lastIndexOf('.') + 1).toLowerCase();
        if (!allowedExt.contains(ext)) {
            throw new BusinessException(400, "不支持的" + label + "类型");
        }
        return ext;
    }
}
