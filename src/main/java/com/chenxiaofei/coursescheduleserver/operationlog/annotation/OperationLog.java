package com.chenxiaofei.coursescheduleserver.operationlog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在需要记录日志的 Controller 方法上，由切面统一落库。
 * 仅记录「写」操作（增删改、登录、状态变更等）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    /** 模块 */
    String module();

    /** 动作 */
    String action();

    /** 详情描述（支持 SpEL，如 "#request.username"，可引用方法参数名） */
    String detail() default "";
}
