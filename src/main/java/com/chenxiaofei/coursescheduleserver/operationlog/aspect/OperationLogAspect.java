package com.chenxiaofei.coursescheduleserver.operationlog.aspect;

import com.chenxiaofei.coursescheduleserver.common.IpUtil;
import com.chenxiaofei.coursescheduleserver.operationlog.entity.OperationLog;
import com.chenxiaofei.coursescheduleserver.operationlog.service.OperationLogService;
import com.chenxiaofei.coursescheduleserver.security.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面：拦截标注了 {@link com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog}
 * 的方法，执行后落库记录（记录失败不影响业务）。
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    /** 参数名发现器（用于 SpEL 详情表达式中引用方法参数名，如 #request.title） */
    private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private static final ExpressionParser parser = new SpelExpressionParser();
    private static final TemplateParserContext templateContext = new TemplateParserContext("{", "}");

    private final OperationLogService operationLogService;

    public OperationLogAspect(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Around("@annotation(com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean success = true;
        String failMsg = null;
        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            success = false;
            failMsg = t.getMessage();
            throw t;
        } finally {
            try {
                record(joinPoint, success, failMsg);
            } catch (Exception e) {
                // 记录日志失败不影响业务
                log.warn("操作日志写入失败：{}", e.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint joinPoint, boolean success, String failMsg) {
        com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog opLog =
                ((MethodSignature) joinPoint.getSignature()).getMethod()
                        .getAnnotation(com.chenxiaofei.coursescheduleserver.operationlog.annotation.OperationLog.class);

        Long userId = UserContext.getUserId();
        String username = UserContext.getUsername();
        String detail = opLog == null ? "" : resolveDetail(opLog.detail(), joinPoint);

        // 目标对象 id：默认取第一个名为 id 的入参（若为基本类型），供列表展示定位
        Long targetId = resolveTargetId(joinPoint);

        OperationLog entry = new OperationLog();
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setModule(opLog == null ? "" : opLog.module());
        entry.setAction(opLog == null ? "" : opLog.action());
        entry.setTargetId(targetId);
        entry.setDetail(success ? detail : (detail.isEmpty() ? failMsg : detail + "（失败：" + failMsg + "）"));
        entry.setIp(IpUtil.resolveIp());
        entry.setSuccess(success);
        operationLogService.record(entry);
    }

    private Long resolveTargetId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        // 遍历参数，找到含 id 字段的简单对象或 Map，取其 id
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof Long) {
                return (Long) arg;
            }
            if (arg instanceof Number) {
                return ((Number) arg).longValue();
            }
            if (arg instanceof java.util.Map) {
                Object id = ((java.util.Map<?, ?>) arg).get("id");
                if (id instanceof Number) {
                    return ((Number) id).longValue();
                }
            }
        }
        return null;
    }

    /**
     * 解析详情模板：支持 SpEL 表达式引用方法参数名（如 #request.title、#week），
     * 模板变量用 {@code {expr}} 包裹。解析失败时回退为原字符串，不影响日志记录。
     */
    private String resolveDetail(String template, ProceedingJoinPoint joinPoint) {
        if (template == null || template.isBlank()) {
            return "";
        }
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
            StandardEvaluationContext context = new StandardEvaluationContext();
            if (paramNames != null) {
                Object[] args = joinPoint.getArgs();
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            return parser.parseExpression(template, templateContext)
                    .getValue(context, String.class);
        } catch (Exception e) {
            log.warn("操作日志详情表达式解析失败：{}", e.getMessage());
            return template;
        }
    }
}
