package com.ai.manager.system.iot.controller;

import com.ai.manager.framework.web.GlobalExceptionHandler;
import com.ai.manager.system.iot.domain.dto.DeviceActivateResult;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.dto.OtaCheckRequest;
import com.ai.manager.system.iot.domain.dto.OtaCheckResponse;
import com.ai.manager.system.iot.service.OtaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DeviceOtaController 设备侧 OTA 接口测试（check/activate/download）。
 */
class DeviceOtaControllerTest {

    private MockMvc mockMvc;
    private OtaService otaService;

    @BeforeEach
    void setUp() {
        otaService = mock(OtaService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new DeviceOtaController(otaService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void check_shouldReturnConfig() throws Exception {
        OtaCheckResponse resp = new OtaCheckResponse();
        resp.setWebsocket(new OtaCheckResponse.WebsocketConfig("ws://host/ws/device", "tok", 3));
        resp.setServerTime(new OtaCheckResponse.ServerTime(1700000000L, 28800));
        when(otaService.check(any(OtaCheckRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/iot/ota/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mac":"aabbccdd","firmware_version":"2.0.0","model":"supermini-c3"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.websocket.url").value("ws://host/ws/device"))
                .andExpect(jsonPath("$.data.websocket.token").value("tok"))
                .andExpect(jsonPath("$.data.websocket.version").value(3))
                .andExpect(jsonPath("$.data.server_time.timezone_offset").value(28800));
    }

    @Test
    void check_shouldIncludeFirmware() throws Exception {
        OtaCheckResponse resp = new OtaCheckResponse();
        resp.setFirmware(new OtaCheckResponse.FirmwareInfo("2.2.1", "http://host/api/iot/ota/download/9", false));
        when(otaService.check(any(OtaCheckRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/iot/ota/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mac\":\"aabbccdd\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firmware.version").value("2.2.1"))
                .andExpect(jsonPath("$.data.firmware.force").value(false));
    }

    @Test
    void activate_shouldReturnResult() throws Exception {
        DeviceActivateResult result = new DeviceActivateResult();
        result.setSuccess(true);
        result.setDeviceId(1L);
        when(otaService.activate(any())).thenReturn(result);

        mockMvc.perform(post("/api/iot/ota/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mac":"aabbccdd","nonce":"n1","timestamp":1700000000,"signature":"sig"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.device_id").value(1));
    }

    @Test
    void download_shouldReturnBinaryWithHeaders() throws Exception {
        when(otaService.getDownloadInfo(9L)).thenReturn(
                new FirmwareDownloadInfo("2.2.1", "abc123", 3, new byte[]{1, 2, 3}));

        mockMvc.perform(get("/api/iot/ota/download/9"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Firmware-Version", "2.2.1"))
                .andExpect(header().string("X-Firmware-Hash", "abc123"))
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }
}
