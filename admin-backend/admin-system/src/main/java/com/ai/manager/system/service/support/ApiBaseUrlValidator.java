package com.ai.manager.system.service.support;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * API BaseUrl 安全校验（SSRF 防护）
 *
 * <p>保存模型 / Embedding 配置时校验 apiBaseUrl，拒绝指向内网 / 环回 / 链路本地 / 云元数据地址的
 * 目标，防止拥有配置权限者把后端发起的模型调用打到内部网络（如 169.254.169.254 元数据端点）。
 * 官方域名与公网主机放行；校验通过主机名实际解析结果判定（域名解析到内网地址同样拦截，兼顾 DNS 重绑定场景）。</p>
 *
 * <p>可用属性 {@code ai-manager.ai-knowledge.validate-api-base-url} 关闭（默认开启，灰度/内网网关场景用）。</p>
 */
@Slf4j
@Component
public class ApiBaseUrlValidator {

    private final boolean enabled;

    /** 单一构造（Spring 自动注入）：属性 ai-manager.ai-knowledge.validate-api-base-url 控制开关，默认开启 */
    ApiBaseUrlValidator(@Value("${ai-manager.ai-knowledge.validate-api-base-url:true}") boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 校验 apiBaseUrl，非法时抛业务异常。
     *
     * @param apiBaseUrl 待校验地址；null / 空白（使用默认地址）直接放行
     */
    public void validate(String apiBaseUrl) {
        if (!enabled || apiBaseUrl == null || apiBaseUrl.isBlank()) {
            return;
        }
        URI uri;
        try {
            uri = URI.create(apiBaseUrl);
        } catch (IllegalArgumentException e) {
            throw invalid("apiBaseUrl 不是合法 URL：" + apiBaseUrl);
        }
        if (uri.getScheme() == null
                || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw invalid("apiBaseUrl 仅支持 http/https：" + apiBaseUrl);
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw invalid("apiBaseUrl 缺少主机名：" + apiBaseUrl);
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isForbiddenAddress(address)) {
                    throw invalid("apiBaseUrl 禁止指向内网/环回/元数据地址（SSRF 防护）：" + apiBaseUrl);
                }
            }
        } catch (UnknownHostException e) {
            throw invalid("apiBaseUrl 主机无法解析：" + host);
        }
    }

    /** 判定地址是否属于禁止范围（内网 / 环回 / 链路本地 / 任意地址） */
    private static boolean isForbiddenAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress();
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
