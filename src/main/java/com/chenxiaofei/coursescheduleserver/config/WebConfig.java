package com.chenxiaofei.coursescheduleserver.config;

import com.chenxiaofei.coursescheduleserver.security.AdminInterceptor;
import com.chenxiaofei.coursescheduleserver.security.JwtAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：跨域 + 鉴权拦截器 + 管理端权限拦截器 + 上传文件静态映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final PathResolver pathResolver;

    public WebConfig(JwtAuthInterceptor jwtAuthInterceptor, AdminInterceptor adminInterceptor, PathResolver pathResolver) {
        this.jwtAuthInterceptor = jwtAuthInterceptor;
        this.adminInterceptor = adminInterceptor;
        this.pathResolver = pathResolver;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 先 JWT 鉴权（填充 UserContext），再管理端角色校验
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/captcha",
                        "/api/auth/reset-code",
                        "/api/auth/reset-password"
                );
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = pathResolver.uploadDir().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
