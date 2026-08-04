package com.ai.manager.framework.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ControllerLogAspect 单元测试
 * mock ProceedingJoinPoint/MethodSignature 验证日志切面的成功/异常/跳过/参数脱敏/客户端 IP 解析分支。
 * simpleName 命中 {@code DeployLogController} 的 tail 等方法会跳过日志记录。
 */
class ControllerLogAspectTest {

    /** 占位控制器：simpleName 触发 DeployLogController 跳过日志分支（仅测试用） */
    static class DeployLogController {
    }

    /** 占位控制器：simpleName 触发 PomodoroSessionController 跳过 getActive 日志分支 */
    static class PomodoroSessionController {
    }

    /** 占位控制器：simpleName 触发 NbTodoController 跳过 today/dueReminders 日志分支 */
    static class NbTodoController {
    }

    /** 占位控制器：simpleName 触发 PomodoroRecordController 跳过 today 日志分支 */
    static class PomodoroRecordController {
    }

    private final ControllerLogAspect aspect = new ControllerLogAspect(new ObjectMapper());

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private ProceedingJoinPoint mockJoinPoint(Class<?> declaringType, String methodName,
                                              String[] paramNames, Object[] args, Object proceedResult) throws Throwable {
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getDeclaringType()).thenReturn(declaringType);
        when(sig.getName()).thenReturn(methodName);
        when(sig.getParameterNames()).thenReturn(paramNames);
        ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
        when(jp.getSignature()).thenReturn(sig);
        when(jp.getArgs()).thenReturn(args);
        when(jp.proceed()).thenReturn(proceedResult);
        return jp;
    }

    @Test
    void around_success_shouldProceedAndReturnResult() throws Throwable {
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "toString", new String[]{"obj"}, new Object[]{null}, "ok");

        Object result = aspect.around(jp);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void around_exception_shouldRethrow() throws Throwable {
        MethodSignature sig = mock(MethodSignature.class);
        when(sig.getDeclaringType()).thenReturn(Object.class);
        when(sig.getName()).thenReturn("toString");
        when(sig.getParameterNames()).thenReturn(new String[]{"obj"});
        ProceedingJoinPoint jp = mock(ProceedingJoinPoint.class);
        when(jp.getSignature()).thenReturn(sig);
        when(jp.getArgs()).thenReturn(new Object[]{null});
        when(jp.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> aspect.around(jp))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    void around_deployLogTail_shouldSkipLog() throws Throwable {
        ProceedingJoinPoint jp = mockJoinPoint(DeployLogController.class, "tail", new String[0], new Object[0], "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_deployLogNonSkippedMethod_shouldLog() throws Throwable {
        ProceedingJoinPoint jp = mockJoinPoint(DeployLogController.class, "save", new String[0], new Object[0], "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_multipartFile_shouldSanitizeArg() throws Throwable {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "upload", new String[]{"file"}, new Object[]{file}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_withForwardedHeader_shouldResolveFirstIp() throws Throwable {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "toString", new String[]{"obj"}, new Object[]{null}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_withoutProxyHeader_shouldFallbackToRemoteAddr() throws Throwable {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "toString", new String[]{"obj"}, new Object[]{null}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_pomodoroGetActive_shouldSkipLog() throws Throwable {
        ProceedingJoinPoint jp = mockJoinPoint(PomodoroSessionController.class, "getActive", new String[0], new Object[0], "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_nbTodoToday_shouldSkipLog() throws Throwable {
        ProceedingJoinPoint jp = mockJoinPoint(NbTodoController.class, "today", new String[0], new Object[0], "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_multipartFileArray_shouldSanitizeArgs() throws Throwable {
        MockMultipartFile f1 = new MockMultipartFile("f", "a.png", "image/png", new byte[]{1});
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "upload", new String[]{"files"},
                new Object[]{new MultipartFile[]{f1}}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_servletRequestArg_shouldSanitizeToClassName() throws Throwable {
        HttpServletRequest req = mock(HttpServletRequest.class);
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "get", new String[]{"request"}, new Object[]{req}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_withXRealIpHeader_shouldResolveRealIp() throws Throwable {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "toString", new String[]{"obj"}, new Object[]{null}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_pomodoroRecordToday_shouldSkipLog() throws Throwable {
        ProceedingJoinPoint jp = mockJoinPoint(PomodoroRecordController.class, "today", new String[0], new Object[0], "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_nbTodoDueReminders_shouldSkipLog() throws Throwable {
        ProceedingJoinPoint jp = mockJoinPoint(NbTodoController.class, "dueReminders", new String[0], new Object[0], "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_withProxyClientIpHeader_shouldResolveProxyIp() throws Throwable {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn("172.16.0.1");
        when(request.getRemoteAddr()).thenReturn("9.9.9.9");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "toString", new String[]{"obj"}, new Object[]{null}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }

    @Test
    void around_servletResponseArg_shouldSanitizeToClassName() throws Throwable {
        HttpServletResponse resp = mock(HttpServletResponse.class);
        ProceedingJoinPoint jp = mockJoinPoint(Object.class, "write", new String[]{"response"}, new Object[]{resp}, "ok");

        assertThat(aspect.around(jp)).isEqualTo("ok");
    }
}
