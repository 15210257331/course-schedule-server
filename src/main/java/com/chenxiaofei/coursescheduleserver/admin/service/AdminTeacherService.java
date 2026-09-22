package com.chenxiaofei.coursescheduleserver.admin.service;

import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherListItem;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherPageRequest;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherStatusRequest;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.common.Pages;
import com.chenxiaofei.coursescheduleserver.mail.MailService;
import com.chenxiaofei.coursescheduleserver.security.PasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * 管理端 - 教师（用户）管理
 */
@Service
@RequiredArgsConstructor
public class AdminTeacherService {

    private final UserMapper userMapper;
    private final MailService mailService;
    private final PasswordService passwordService;

    /** 教师分页（含统计） */
    public PageResult<TeacherListItem> page(TeacherPageRequest req) {
        return Pages.of(req,
                () -> userMapper.countTeachers(req.getStatus(), req.getKeyword()),
                (offset, limit) -> userMapper.pageTeacherItems(req.getStatus(), req.getKeyword(), offset, limit));
    }

    /** 教师详情 */
    public User detail(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(404, "教师不存在");
        }
        user.setPassword(null);
        return user;
    }

    /** 禁用/启用 */
    public void updateStatus(TeacherStatusRequest req) {
        User user = userMapper.findById(req.getId());
        if (user == null) {
            throw new BusinessException(404, "教师不存在");
        }
        if (!"active".equals(req.getStatus()) && !"disabled".equals(req.getStatus())) {
            throw new BusinessException("非法状态");
        }
        String reason = "disabled".equals(req.getStatus())
                ? (req.getReason() == null ? "" : req.getReason().trim())
                : null;
        userMapper.updateStatus(req.getId(), req.getStatus(), reason);
    }

    /** 重置密码：生成随机密码并发送到教师邮箱 */
    public void resetPassword(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(404, "教师不存在");
        }
        String newPassword = randomPassword(10);
        userMapper.updatePassword(id, passwordService.encode(newPassword));
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            mailService.sendAdminResetPassword(user.getEmail(), user.getNickname(), newPassword);
        }
    }

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private String randomPassword(int len) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
