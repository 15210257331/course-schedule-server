package com.chenxiaofei.coursescheduleserver.auth.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 邮箱验证码内存存储：验证码 5 分钟过期、同一邮箱 60 秒内只能发送一次、最多校验 5 次。
 * 进程内存储，重启即失效（单实例部署下足够）。
 */
@Component
public class CaptchaStore {

    private static final int EXPIRES_MINUTES = 5;
    private static final int RESEND_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final ConcurrentMap<String, Entry> store = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static class Entry {
        final String code;
        final LocalDateTime expiresAt;
        final LocalDateTime lastSendAt;
        int attempts;

        Entry(String code, LocalDateTime expiresAt, LocalDateTime lastSendAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.lastSendAt = lastSendAt;
        }
    }

    /** 生成 6 位数字验证码并存入；60 秒内重复请求则拒绝 */
    public synchronized String issue(String email) {
        Entry existing = store.get(email);
        if (existing != null && existing.lastSendAt.plusSeconds(RESEND_SECONDS).isAfter(LocalDateTime.now())) {
            throw new BusinessException(429, "发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", random.nextInt(1000000));
        store.put(email, new Entry(code, LocalDateTime.now().plusMinutes(EXPIRES_MINUTES), LocalDateTime.now()));
        return code;
    }

    /** 校验验证码；成功即消耗（清除），失败累计次数，超过上限作废 */
    public synchronized void verify(String email, String input) {
        Entry e = store.get(email);
        if (e == null) {
            throw new BusinessException(400, "验证码不存在或已过期，请重新获取");
        }
        if (e.expiresAt.isBefore(LocalDateTime.now())) {
            store.remove(email);
            throw new BusinessException(400, "验证码已过期，请重新获取");
        }
        if (e.attempts >= MAX_ATTEMPTS) {
            store.remove(email);
            throw new BusinessException(400, "验证码尝试次数过多，请重新获取");
        }
        if (!e.code.equals(input == null ? "" : input.trim())) {
            e.attempts++;
            throw new BusinessException(400, "验证码错误");
        }
        store.remove(email);
    }
}