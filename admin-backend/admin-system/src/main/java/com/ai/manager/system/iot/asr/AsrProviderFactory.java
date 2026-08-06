package com.ai.manager.system.iot.asr;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.config.IotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * ASR provider 工厂：按 {@code IotProperties.asr.type} 返回对应实现，未知 type 抛业务异常。
 * <p>
 * 新增厂商时在此注册即可，上层（语音流水线）只依赖 {@link AsrProvider} 抽象。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsrProviderFactory {

    private final IotProperties iotProperties;
    private final OpenAiCompatibleAsrProvider openAiCompatibleAsrProvider;

    /**
     * 返回当前配置对应的 ASR provider 实例。
     *
     * @throws BusinessException type 未配置或未知时抛出
     */
    public AsrProvider getProvider() {
        String type = StringUtils.hasText(iotProperties.getAsr().getType())
                ? iotProperties.getAsr().getType()
                : OpenAiCompatibleAsrProvider.TYPE;
        AsrProvider provider = switch (type) {
            case OpenAiCompatibleAsrProvider.TYPE, "openai" -> openAiCompatibleAsrProvider;
            default -> null;
        };
        if (provider == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(),
                    "不支持的 ASR provider 类型: " + type);
        }
        return provider;
    }
}
