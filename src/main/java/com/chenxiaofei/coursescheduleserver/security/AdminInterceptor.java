package com.chenxiaofei.coursescheduleserver.security;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端权限拦截器：校验当前登录用户是否为 ADMIN 角色。
 * 在 WebConfig 中注册到 /api/admin/**，位于 JwtAuthInterceptor 之后，
 * JWT 先解析 token 填充 UserContext，此处再校验角色。
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BusinessException(403, "无权限访问，仅管理员可操作");
        }
        return true;
    }
}
