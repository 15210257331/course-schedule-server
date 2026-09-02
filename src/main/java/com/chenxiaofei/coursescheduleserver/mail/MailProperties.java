package com.chenxiaofei.coursescheduleserver.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件发送配置（app 层开关）：enabled 为 false 时不接真实 SMTP，仅打印验证码到日志，
 * 便于开发环境联调；生产开启后走标准 spring.mail.* 配置。
 */
@Component
@ConfigurationProperties(prefix = "mail")
@Data
public class MailProperties {

    /** 是否真实发送邮件（false = 开发模式，验证码打印到日志） */
    private boolean enabled = false;
    /** 发件人地址（真实发送时使用） */
    private String from = "";
}