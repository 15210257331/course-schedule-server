package com.chenxiaofei.coursescheduleserver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.chenxiaofei.coursescheduleserver.config.JwtProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_NICKNAME = "nickname";

    public JwtUtil(JwtProperties properties) {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret 长度必须 >= 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = properties.getExpireSeconds() * 1000;
    }

    public String generate(Long userId, String role, String nickname) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_NICKNAME, nickname)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expireMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }
}