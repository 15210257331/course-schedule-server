package com.chenxiaofei.coursescheduleserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自动备份配置：本地存储目录 + 腾讯云 COS 开关。
 * 对应 application.yaml 中的 app.backup.* 配置项。
 */
@Component
@ConfigurationProperties(prefix = "app.backup")
public class BackupProperties {

    /** 备份文件本地目录（cos.enabled=false 时使用；相对路径基于运行目录） */
    private String localDir = "./backup";

    /** COS 配置 */
    private Cos cos = new Cos();

    public String getLocalDir() {
        return localDir;
    }

    public void setLocalDir(String localDir) {
        this.localDir = localDir;
    }

    public Cos getCos() {
        return cos;
    }

    public void setCos(Cos cos) {
        this.cos = cos;
    }

    public static class Cos {

        /** 全局开关：true 上传到腾讯云 COS，false 保存到本地服务器 */
        private boolean enabled = false;

        private String secretId = "";
        private String secretKey = "";
        private String region = "ap-guangzhou";
        /** 存储桶名称，格式：bucketname-appid */
        private String bucket = "";
        /** 备份文件在桶内的目录前缀 */
        private String prefix = "backups/";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getSecretId() {
            return secretId;
        }

        public void setSecretId(String secretId) {
            this.secretId = secretId;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }
    }
}