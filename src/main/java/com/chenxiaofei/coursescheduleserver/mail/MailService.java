package com.chenxiaofei.coursescheduleserver.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件发送服务：enabled=true 时通过 SMTP 发送；否则仅打印验证码到日志（开发模式）。
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final MailProperties properties;
    private final ObjectProvider<JavaMailSender> mailSender;

    public MailService(MailProperties properties, ObjectProvider<JavaMailSender> mailSender) {
        this.properties = properties;
        this.mailSender = mailSender;
    }

    public void sendResetCode(String to, String code) {
        String subject = "TeacherOS 密码重置验证码";
        String text = "您的验证码为：" + code + "，5 分钟内有效。若非本人操作请忽略。";
        if (!properties.isEnabled()) {
            log.info("[开发模式] 密码重置验证码 -> {}：{}", to, code);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(properties.getFrom());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.getObject().send(msg);
    }

    /** 管理员重置教师密码后，将新密码发送到教师邮箱 */
    public void sendAdminResetPassword(String to, String nickname, String newPassword) {
        String subject = "TeacherOS 密码重置通知";
        String text = "尊敬的 " + (nickname == null ? "老师" : nickname) + "：\n\n"
                + "您的账号密码已由管理员重置，新密码为：" + newPassword + "\n\n"
                + "请尽快登录并修改密码。若非本人操作请联系平台管理员。";
        if (!properties.isEnabled()) {
            log.info("[开发模式] 管理员重置密码 -> {}：{}", to, newPassword);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(properties.getFrom());
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        mailSender.getObject().send(msg);
    }
}