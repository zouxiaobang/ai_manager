package com.ai.manager.system.iot.asr;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.config.IotProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class AsrProviderFactoryTest {

    private final OpenAiCompatibleAsrProvider openAiProvider = mock(OpenAiCompatibleAsrProvider.class);

    @Test
    void openaiCompatible_shouldReturnOpenAiProvider() {
        IotProperties props = new IotProperties();
        props.getAsr().setType("openai-compatible");

        AsrProvider provider = new AsrProviderFactory(props, openAiProvider).getProvider();

        assertThat(provider).isSameAs(openAiProvider);
    }

    @Test
    void openaiAlias_shouldAlsoReturnOpenAiProvider() {
        IotProperties props = new IotProperties();
        props.getAsr().setType("openai");

        AsrProvider provider = new AsrProviderFactory(props, openAiProvider).getProvider();

        assertThat(provider).isSameAs(openAiProvider);
    }

    @Test
    void blankType_shouldFallbackToOpenAiCompatible() {
        IotProperties props = new IotProperties();
        props.getAsr().setType("");

        AsrProvider provider = new AsrProviderFactory(props, openAiProvider).getProvider();

        assertThat(provider).isSameAs(openAiProvider);
    }

    @Test
    void unknownType_shouldThrowBusinessException() {
        IotProperties props = new IotProperties();
        props.getAsr().setType("baidu");

        AsrProviderFactory factory = new AsrProviderFactory(props, openAiProvider);

        assertThatThrownBy(factory::getProvider)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("baidu");
    }
}
