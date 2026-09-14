package com.chenxiaofei.coursescheduleserver.auth.service;

import com.chenxiaofei.coursescheduleserver.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防暴力破解：按「用户名」与「来源IP」双维度计数。
 * 连续失败达到上限后锁定一段时间；登录成功后清零。
 * 进程内存储，重启即失效（单实例部署下足够）。
 */
@Component
public class LoginAttemptGuard {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptGuard.class);

    /** 允许的最大连续失败次数 */
    private static final int MAX_FAILURES = 5;
    /** 达到上限后的锁定时长（分钟） */
    private static final long LOCK_MINUTES = 15;
    /** 失败计数窗口：超过该时间未再失败则重新计数（分钟） */
    private static final long WINDOW_MINUTES = 30;

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    private static class Entry {
        int failures;
        LocalDateTime lockedUntil;
        LocalDateTime lastFailAt;
    }

    /** 登录前校验：若当前账号/来源已被锁定，直接拒绝 */
    public void check(String username, String ip) {
        Entry e = store.get(key(username, ip));
        if (e != null && e.lockedUntil != null && e.lockedUntil.isAfter(LocalDateTime.now())) {
            long remain = java.time.Duration.between(LocalDateTime.now(), e.lockedUntil).toSeconds();
            throw new BusinessException(429, "登录失败次数过多，账号已锁定，请 " + (remain / 60 + 1) + " 分钟后再试");
        }
    }

    /** 记录一次失败，达到上限则锁定 */
    public void onFailure(String username, String ip) {
        String key = key(username, ip);
        Entry e = store.computeIfAbsent(key, k -> new Entry());
        LocalDateTime now = LocalDateTime.now();
        // 超窗未再失败则重置计数
        if (e.lastFailAt != null && e.lastFailAt.plusMinutes(WINDOW_MINUTES).isBefore(now)) {
            e.failures = 0;
        }
        e.failures++;
        e.lastFailAt = now;
        if (e.failures >= MAX_FAILURES) {
            e.lockedUntil = now.plusMinutes(LOCK_MINUTES);
            log.warn("登录防暴力破解：账号 {}（IP {}）连续失败 {} 次，锁定 {} 分钟", username, ip, e.failures, LOCK_MINUTES);
        }
    }

    /** 登录成功后清零 */
    public void onSuccess(String username, String ip) {
        store.remove(key(username, ip));
    }

    private String key(String username, String ip) {
        return (username == null ? "" : username.trim()) + "|" + (ip == null ? "" : ip);
    }
}
