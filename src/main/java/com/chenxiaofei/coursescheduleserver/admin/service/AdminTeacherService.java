package com.chenxiaofei.coursescheduleserver.admin.service;

import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherListItem;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherPageRequest;
import com.chenxiaofei.coursescheduleserver.admin.dto.TeacherStatusRequest;
import com.chenxiaofei.coursescheduleserver.auth.entity.User;
import com.chenxiaofei.coursescheduleserver.auth.mapper.UserMapper;
import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import com.chenxiaofei.coursescheduleserver.common.PageResult;
import com.chenxiaofei.coursescheduleserver.mail.MailService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

/**
 * 管理端 - 教师（用户）管理
 */
@Service
public class AdminTeacherService {

    private final UserMapper userMapper;
    private final MailService mailService;

    public AdminTeacherService(UserMapper userMapper, MailService mailService) {
        this.userMapper = userMapper;
        this.mailService = mailService;
    }

    /** 教师分页（含统计） */
    public PageResult<TeacherListItem> page(TeacherPageRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 20 : req.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;
        long total = userMapper.countTeachers(req.getStatus(), req.getKeyword());
        List<TeacherListItem> list = userMapper.pageTeacherItems(req.getStatus(), req.getKeyword(), offset, pageSize);
        return PageResult.of(total, list);
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
        userMapper.updatePassword(id, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
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
