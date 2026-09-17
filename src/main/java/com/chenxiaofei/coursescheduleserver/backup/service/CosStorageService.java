package com.chenxiaofei.coursescheduleserver.backup.service;

import com.chenxiaofei.coursescheduleserver.config.BackupProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.util.Date;

/**
 * 腾讯云 COS 对象存储：备份文件上传（异地备份）。
 * 仅当 {@link BackupProperties.Cos#isEnabled()} 为 true 时使用；
 * 客户端按需创建、用完关闭。
 */
@Service
public class CosStorageService {

    private static final Logger log = LoggerFactory.getLogger(CosStorageService.class);

    private final BackupProperties properties;

    public CosStorageService(BackupProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.getCos().isEnabled();
    }

    /**
     * 上传本地文件到 COS，返回对象 key（不含桶名，形如 backups/full-backup-xxx.json）。
     *
     * @param file     待上传的本地文件
     * @param fileName 桶内对象名，与本地文件名无关（本地可能是 .tmp- 临时文件）
     */
    public String upload(File file, String fileName) {
        BackupProperties.Cos cos = properties.getCos();
        if (cos.getSecretId().isBlank() || cos.getSecretKey().isBlank() || cos.getBucket().isBlank()) {
            throw new IllegalStateException("COS 未配置 secret-id/secret-key/bucket，无法上传备份");
        }
        String key = keyFor(fileName);
        COSClient client = client();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            metadata.setContentType("application/json");
            PutObjectRequest request = new PutObjectRequest(cos.getBucket(), key, file);
            request.setMetadata(metadata);
            client.putObject(request);
            log.info("备份文件已上传到 COS：{}/{}", cos.getBucket(), key);
            return key;
        } finally {
            client.shutdown();
        }
    }

    /** 生成临时下载链接（签名 URL，有效期 minutes 分钟） */
    public String presignUrl(String key, int minutes) {
        BackupProperties.Cos cos = properties.getCos();
        COSClient client = client();
        try {
            Date expiration = new Date(System.currentTimeMillis() + minutes * 60_000L);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(cos.getBucket(), key, HttpMethodName.GET);
            request.setExpiration(expiration);
            URL url = client.generatePresignedUrl(request);
            return url.toString();
        } finally {
            client.shutdown();
        }
    }

    private String keyFor(String fileName) {
        String prefix = properties.getCos().getPrefix();
        if (prefix == null || prefix.isBlank()) {
            return fileName;
        }
        return prefix.endsWith("/") ? prefix + fileName : prefix + "/" + fileName;
    }

    private COSClient client() {
        BackupProperties.Cos cos = properties.getCos();
        BasicCOSCredentials cred = new BasicCOSCredentials(cos.getSecretId(), cos.getSecretKey());
        Region region = new Region(cos.getRegion());
        return new COSClient(cred, new ClientConfig(region));
    }
}