package com.ai.manager.framework.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ControllerLogAspect {

    private final ObjectMapper objectMapper;

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void restControllerPointcut() {
    }

    @Around("restControllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
        Map<String, Object> params = buildParamMap(signature.getParameterNames(), joinPoint.getArgs());
        log.info("接口请求 ==> {} IP: {} 入参: {}", methodName, resolveClientIp(), toJson(params));

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            log.info("接口响应 <== {} 出参: {} 耗时: {}ms", methodName, toJson(result), System.currentTimeMillis() - start);
            return result;
        } catch (Throwable ex) {
            log.warn("接口异常 <== {} 耗时: {}ms 异常: {}", methodName, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }

    private Map<String, Object> buildParamMap(String[] names, Object[] args) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (args == null || args.length == 0) {
            return map;
        }
        for (int i = 0; i < args.length; i++) {
            String key = names != null && i < names.length ? names[i] : "arg" + i;
            map.put(key, sanitizeArg(args[i]));
        }
        return map;
    }

    private Object sanitizeArg(Object arg) {
        if (arg == null) {
            return null;
        }
        if (arg instanceof MultipartFile file) {
            return "MultipartFile[name=" + file.getOriginalFilename() + ", size=" + file.getSize() + "]";
        }
        if (arg instanceof MultipartFile[] files) {
            String[] descriptions = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                descriptions[i] = (String) sanitizeArg(files[i]);
            }
            return descriptions;
        }
        if (arg instanceof ServletRequest || arg instanceof ServletResponse) {
            return arg.getClass().getSimpleName();
        }
        return arg;
    }

    private String resolveClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                int comma = ip.indexOf(',');
                return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            return String.valueOf(obj);
        }
    }
}
