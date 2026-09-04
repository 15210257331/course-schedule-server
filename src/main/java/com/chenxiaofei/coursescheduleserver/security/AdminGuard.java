package com.chenxiaofei.coursescheduleserver.security;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import org.springframework.stereotype.Component;

/**
 * 管理员权限校验：校验当前登录用户是否为 ADMIN 角色
 */
@Component
public class AdminGuard {

    public void requireAdmin() {
        String role = UserContext.getRole();
        if (!"ADMIN".equals(role)) {
            throw new BusinessException(403, "无权限访问，仅管理员可操作");
        }
    }
}
