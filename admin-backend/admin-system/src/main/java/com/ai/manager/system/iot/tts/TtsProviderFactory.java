package com.ai.manager.system.iot.tts;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.config.IotProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * TTS provider 工厂：按 {@code IotProperties.tts.type} 返回对应实现，未知 type 抛业务异常。
 * <p>
 * 新增厂商时在此注册即可，上层（语音流水线）只依赖 {@link TtsProvider} 抽象。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TtsProviderFactory {

    private final IotProperties iotProperties;
    private final OpenAiCompatibleTtsProvider openAiCompatibleTtsProvider;

    /**
     * 返回当前配置对应的 TTS provider 实例。
     *
     * @throws BusinessException type 未配置或未知时抛出
     */
    public TtsProvider getProvider() {
        String type = StringUtils.hasText(iotProperties.getTts().getType())
                ? iotProperties.getTts().getType()
                : OpenAiCompatibleTtsProvider.TYPE;
        TtsProvider provider = switch (type) {
            case OpenAiCompatibleTtsProvider.TYPE, "openai" -> openAiCompatibleTtsProvider;
            default -> null;
        };
        if (provider == null) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(),
                    "不支持的 TTS provider 类型: " + type);
        }
        return provider;
    }
}
