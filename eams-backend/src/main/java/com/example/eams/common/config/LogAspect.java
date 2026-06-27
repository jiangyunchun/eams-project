package com.example.eams.common.config;

import cn.hutool.json.JSONUtil;
import com.example.eams.security.filter.SecurityContextHolder;
import com.example.eams.system.entity.SysOperationLog;
import com.example.eams.system.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 操作日志 AOP 切面
 * 拦截 @OperationLog 注解，自动记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperationLogMapper operationLogMapper;

    @Around("@annotation(com.example.eams.common.config.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取注解信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog opLog = method.getAnnotation(OperationLog.class);

        // 执行原方法
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            recordLog(opLog, joinPoint, System.currentTimeMillis() - startTime, e.getMessage());
            throw e;
        }

        recordLog(opLog, joinPoint, System.currentTimeMillis() - startTime, null);
        return result;
    }

    private void recordLog(OperationLog opLog, ProceedingJoinPoint joinPoint, long costTime, String errorMsg) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

            SysOperationLog logRecord = new SysOperationLog();
            logRecord.setOperator(SecurityContextHolder.getCurrentUsername());
            logRecord.setModule(opLog.module());
            logRecord.setActionType(opLog.actionType());

            // 构建描述
            String desc = buildDescription(opLog.description(), joinPoint.getArgs());
            logRecord.setDescription(desc);

            // 请求参数
            logRecord.setRequestParams(JSONUtil.toJsonStr(joinPoint.getArgs()));
            logRecord.setIpAddress(getIp(request));
            logRecord.setUserAgent(request.getHeader("User-Agent"));
            logRecord.setCostTime(costTime);

            operationLogMapper.insert(logRecord);
        } catch (Exception e) {
            log.warn("操作日志记录失败", e);
        }
    }

    private String buildDescription(String template, Object[] args) {
        if (template == null || template.isEmpty()) return "";
        String desc = template;
        for (int i = 0; i < (args != null ? args.length : 0); i++) {
            if (args[i] != null) {
                desc = desc.replace("{" + i + "}", args[i].toString());
            }
        }
        return desc;
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        // IPv6 本地回环地址转 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
        return ip;
    }
}
