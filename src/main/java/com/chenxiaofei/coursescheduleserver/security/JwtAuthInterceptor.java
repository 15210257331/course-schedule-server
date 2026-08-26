package com.chenxiaofei.coursescheduleserver.security;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        String token = auth.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            String role = claims.get("role", String.class);
            UserContext.set(userId, role);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(401, "登录状态无效，请重新登录");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}