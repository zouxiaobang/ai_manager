package com.ai.manager.system.iot.service;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.system.iot.asr.AsrContext;
import com.ai.manager.system.iot.asr.AsrProvider;
import com.ai.manager.system.iot.asr.AsrResult;
import com.ai.manager.system.iot.audio.OpusAudioCodec;
import com.ai.manager.system.iot.audio.WavUtil;
import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.tts.TtsContext;
import com.ai.manager.system.iot.tts.TtsProvider;
import com.ai.manager.system.iot.tts.TtsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoicePipelineServiceTest {

    @Mock
    private AsrProvider asrProvider;

    @Mock
    private TtsProvider ttsProvider;

    @Mock
    private OpusAudioCodec codec;

    private IotProperties properties;
    private VoicePipelineService service;

    @BeforeEach
    void setUp() {
        properties = new IotProperties();
        properties.getAsr().setLanguage("zh");
        properties.getTts().setVoice("nova");
        // 默认回显模式为开，走 ASR/TTS 的用例需显式关闭
        properties.getVoice().setEchoMode(false);
        service = new VoicePipelineService(asrProvider, ttsProvider, codec, properties);
    }

    @Test
    void processTurn_inEchoMode_shouldSkipAsrAndTtsAndEchoBack() {
        properties.getVoice().setEchoMode(true);
        byte[] deviceOpus = new byte[]{1, 2, 3};
        short[] pcm = new short[]{100, 200, 300};
        byte[] downlink = new byte[]{0, 2, 9, 8};
        when(codec.decodeToPcm(eq(deviceOpus), eq(16000))).thenReturn(pcm);
        when(codec.encodePcm(eq(pcm), eq(16000))).thenReturn(downlink);

        byte[] result = service.processTurn(deviceOpus, "s1");

        assertThat(result).isEqualTo(downlink);
        verify(codec).decodeToPcm(deviceOpus, 16000);
        verify(codec).encodePcm(pcm, 16000);
        // 回显模式不触碰 ASR/TTS
        verifyNoInteractions(asrProvider, ttsProvider);
    }

    @Test
    void processTurn_shouldOrchestrateOpusToAsrToTtsAndReturnOpus() {
        byte[] deviceOpus = new byte[]{1, 2, 3};
        short[] pcm = new short[]{100, 200, 300};
        byte[] wav = WavUtil.pcmToWav(pcm, 16000, 1, 16);
        when(codec.decodeToPcm(eq(deviceOpus), eq(16000))).thenReturn(pcm);
        when(asrProvider.transcribe(eq(wav), argThat(ctx -> "s1".equals(ctx.sessionId())
                && "zh".equals(ctx.language()) && ctx.sampleRate() == 16000)))
                .thenReturn(new AsrResult("你好", "{}", 12));

        byte[] ttsWav = WavUtil.pcmToWav(new short[]{1, 2, 3}, 24000, 1, 16);
        when(ttsProvider.synthesize(eq("你好"), argThat(ctx -> "s1".equals(ctx.sessionId())
                && "nova".equals(ctx.voice()) && ctx.sampleRate() == 16000)))
                .thenReturn(new TtsResult(ttsWav, "wav", 24000, 20));

        short[] ttsPcm = WavUtil.wavToPcm(ttsWav);
        byte[] downlink = new byte[]{9, 8, 7};
        when(codec.encodePcm(eq(ttsPcm), eq(16000))).thenReturn(downlink);

        byte[] result = service.processTurn(deviceOpus, "s1");

        assertThat(result).isEqualTo(downlink);
        verify(codec).decodeToPcm(deviceOpus, 16000);
        verify(asrProvider).transcribe(eq(wav), any(AsrContext.class));
        verify(ttsProvider).synthesize(eq("你好"), any(TtsContext.class));
        verify(codec).encodePcm(eq(ttsPcm), eq(16000));
    }

    @Test
    void processTurn_whenTtsReturnsOpusFormat_shouldRejectWithBusinessException() {
        byte[] deviceOpus = new byte[]{1};
        when(codec.decodeToPcm(eq(deviceOpus), eq(16000))).thenReturn(new short[]{100});
        when(asrProvider.transcribe(any(byte[].class), any(AsrContext.class)))
                .thenReturn(new AsrResult("hi", "{}", 5));
        when(ttsProvider.synthesize(eq("hi"), any(TtsContext.class)))
                .thenReturn(new TtsResult(new byte[]{4, 5}, "opus", 24000, 8));

        assertThatThrownBy(() -> service.processTurn(deviceOpus, "s1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("opus");
    }

    @Test
    void processTurn_shouldPassLanguageAndVoiceFromProperties() {
        byte[] deviceOpus = new byte[]{1};
        when(codec.decodeToPcm(eq(deviceOpus), eq(16000))).thenReturn(new short[]{50});
        when(asrProvider.transcribe(any(byte[].class), any(AsrContext.class)))
                .thenReturn(new AsrResult("ok", "{}", 3));
        byte[] ttsWav = WavUtil.pcmToWav(new short[]{9}, 16000, 1, 16);
        when(ttsProvider.synthesize(eq("ok"), any(TtsContext.class)))
                .thenReturn(new TtsResult(ttsWav, "wav", 16000, 3));
        when(codec.encodePcm(any(short[].class), eq(16000))).thenReturn(new byte[]{0});

        service.processTurn(deviceOpus, "s2");

        ArgumentCaptor<AsrContext> asrCtx = ArgumentCaptor.forClass(AsrContext.class);
        verify(asrProvider).transcribe(any(byte[].class), asrCtx.capture());
        assertThat(asrCtx.getValue().language()).isEqualTo("zh");
        assertThat(asrCtx.getValue().sessionId()).isEqualTo("s2");

        ArgumentCaptor<TtsContext> ttsCtx = ArgumentCaptor.forClass(TtsContext.class);
        verify(ttsProvider).synthesize(eq("ok"), ttsCtx.capture());
        assertThat(ttsCtx.getValue().voice()).isEqualTo("nova");
        assertThat(ttsCtx.getValue().sessionId()).isEqualTo("s2");
    }
}
