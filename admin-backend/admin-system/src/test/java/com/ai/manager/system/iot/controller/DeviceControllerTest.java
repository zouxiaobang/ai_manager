package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.PageUtils;
import com.ai.manager.framework.web.GlobalExceptionHandler;
import com.ai.manager.system.iot.domain.dto.DeviceBindRequest;
import com.ai.manager.system.iot.domain.dto.DeviceUpdateRequest;
import com.ai.manager.system.iot.domain.vo.DeviceOnlineStatusVO;
import com.ai.manager.system.iot.domain.vo.DeviceVO;
import com.ai.manager.system.iot.service.DeviceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DeviceController 接口层单元测试（standaloneSetup）。
 */
class DeviceControllerTest {

    private MockMvc mockMvc;
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = mock(DeviceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new DeviceController(deviceService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private DeviceVO vo(Long id, String mac) {
        DeviceVO vo = new DeviceVO();
        vo.setId(id);
        vo.setMac(mac);
        vo.setStatus("BOUND");
        return vo;
    }

    @Test
    void list_shouldReturnPagedDevices() throws Exception {
        when(deviceService.listDevices(1L, 20L, null, null))
                .thenReturn(PageUtils.of(List.of(vo(1L, "aabbccdd")), 1, 1, 20));

        mockMvc.perform(get("/api/iot/device"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].mac").value("aabbccdd"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void get_shouldReturnDevice() throws Exception {
        when(deviceService.getDevice(1L)).thenReturn(vo(1L, "aabbccdd"));

        mockMvc.perform(get("/api/iot/device/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void bind_withValidBody_shouldReturnDevice() throws Exception {
        when(deviceService.bind(any(DeviceBindRequest.class))).thenReturn(vo(1L, "aabbccdd"));

        mockMvc.perform(post("/api/iot/device/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mac": "AA:BB:CC:DD",
                                  "model": "supermini-c3"
                                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(deviceService).bind(any(DeviceBindRequest.class));
    }

    @Test
    void bind_withMissingMac_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/iot/device/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void online_shouldReturnStatus() throws Exception {
        DeviceOnlineStatusVO status = new DeviceOnlineStatusVO();
        status.setOnline(true);
        when(deviceService.probeOnline(1L)).thenReturn(status);

        mockMvc.perform(get("/api/iot/device/1/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.online").value(true));
        verify(deviceService).probeOnline(1L);
    }

    @Test
    void update_shouldPersist() throws Exception {
        when(deviceService.update(eq(1L), any(DeviceUpdateRequest.class))).thenReturn(vo(1L, "aabbccdd"));

        mockMvc.perform(put("/api/iot/device/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"model": "kyle-s3-lcd"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(deviceService).update(eq(1L), any(DeviceUpdateRequest.class));
    }

    @Test
    void updateStatus_shouldPersist() throws Exception {
        when(deviceService.updateStatus(1L, "ONLINE")).thenReturn(vo(1L, "aabbccdd"));

        mockMvc.perform(put("/api/iot/device/1/status").param("status", "ONLINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BOUND"));
        verify(deviceService).updateStatus(1L, "ONLINE");
    }

    @Test
    void reboot_shouldReturnDevice() throws Exception {
        when(deviceService.reboot(1L)).thenReturn(vo(1L, "aabbccdd"));

        mockMvc.perform(post("/api/iot/device/1/reboot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(deviceService).reboot(1L);
    }
}
