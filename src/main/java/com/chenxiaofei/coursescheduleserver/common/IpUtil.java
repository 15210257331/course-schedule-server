package com.chenxiaofei.coursescheduleserver.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 客户端 IP 解析工具：优先取 X-Forwarded-For，回退到 remoteAddr。
 */
public final class IpUtil {

    private IpUtil() {
    }

    public static String resolveIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                int idx = ip.indexOf(',');
                return (idx > 0 ? ip.substring(0, idx) : ip).trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
