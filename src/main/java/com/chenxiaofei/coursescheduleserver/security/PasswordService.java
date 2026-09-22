package com.chenxiaofei.coursescheduleserver.security;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 密码哈希与校验：集中封装 jbcrypt 调用，避免散落在各业务类。
 */
@Component
public class PasswordService {

    /** 对明文密码哈希（带随机盐） */
    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /** 校验明文密码与已哈希密码是否匹配 */
    public boolean matches(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
