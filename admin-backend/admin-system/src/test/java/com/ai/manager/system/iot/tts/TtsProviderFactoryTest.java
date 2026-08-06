package com.ai.manager.system.iot.tts;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.config.IotProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TtsProviderFactoryTest {

    private final OpenAiCompatibleTtsProvider openAiProvider = mock(OpenAiCompatibleTtsProvider.class);

    @Test
    void openaiCompatible_shouldReturnOpenAiProvider() {
        IotProperties props = new IotProperties();
        props.getTts().setType("openai-compatible");

        TtsProvider provider = new TtsProviderFactory(props, openAiProvider).getProvider();

        assertThat(provider).isSameAs(openAiProvider);
    }

    @Test
    void openaiAlias_shouldAlsoReturnOpenAiProvider() {
        IotProperties props = new IotProperties();
        props.getTts().setType("openai");

        TtsProvider provider = new TtsProviderFactory(props, openAiProvider).getProvider();

        assertThat(provider).isSameAs(openAiProvider);
    }

    @Test
    void blankType_shouldFallbackToOpenAiCompatible() {
        IotProperties props = new IotProperties();
        props.getTts().setType("  ");

        TtsProvider provider = new TtsProviderFactory(props, openAiProvider).getProvider();

        assertThat(provider).isSameAs(openAiProvider);
    }

    @Test
    void unknownType_shouldThrowBusinessException() {
        IotProperties props = new IotProperties();
        props.getTts().setType("azure-unknown");

        TtsProviderFactory factory = new TtsProviderFactory(props, openAiProvider);

        assertThatThrownBy(factory::getProvider)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("azure-unknown");
    }
}
