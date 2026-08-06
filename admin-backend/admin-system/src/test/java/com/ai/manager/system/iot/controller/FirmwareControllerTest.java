package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.framework.web.GlobalExceptionHandler;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;
import com.ai.manager.system.iot.service.FirmwareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FirmwareController 接口层单元测试（standaloneSetup，含 multipart 上传）。
 */
class FirmwareControllerTest {

    private MockMvc mockMvc;
    private FirmwareService firmwareService;

    @BeforeEach
    void setUp() {
        firmwareService = mock(FirmwareService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FirmwareController(firmwareService))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private FirmwareVO vo(Long id, String version) {
        FirmwareVO vo = new FirmwareVO();
        vo.setId(id);
        vo.setVersion(version);
        vo.setStatus("DRAFT");
        return vo;
    }

    @Test
    void upload_withMultipart_shouldReturnFirmware() throws Exception {
        when(firmwareService.upload(any(), any(), any(), anyInt())).thenReturn(vo(1L, "2.2.1"));
        MockMultipartFile file = new MockMultipartFile("file", "fw.bin", MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/iot/firmware/upload")
                        .file(file)
                        .param("version", "2.2.1")
                        .param("force", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.version").value("2.2.1"));
        verify(firmwareService).upload(any(byte[].class), any(), any(), anyInt());
    }

    @Test
    void publish_shouldReturnFirmware() throws Exception {
        when(firmwareService.publish(1L)).thenReturn(vo(1L, "2.2.1"));

        mockMvc.perform(post("/api/iot/firmware/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
        verify(firmwareService).publish(1L);
    }

    @Test
    void forceUpgrade_shouldReturnFirmware() throws Exception {
        when(firmwareService.forceUpgrade(1L)).thenReturn(vo(1L, "2.2.1"));

        mockMvc.perform(post("/api/iot/firmware/1/force"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(firmwareService).forceUpgrade(1L);
    }

    @Test
    void delete_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/iot/firmware/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        verify(firmwareService).delete(1L);
    }

    @Test
    void list_shouldReturnPagedFirmwares() throws Exception {
        when(firmwareService.listFirmwares(eq(1L), eq(20L), eq(null)))
                .thenReturn(PageUtils.of(List.of(vo(1L, "2.2.1")), 1, 1, 20));

        mockMvc.perform(get("/api/iot/firmware"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].version").value("2.2.1"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void get_shouldReturnFirmware() throws Exception {
        when(firmwareService.getFirmware(1L)).thenReturn(vo(1L, "2.2.1"));

        mockMvc.perform(get("/api/iot/firmware/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("2.2.1"));
    }

    @Test
    void otaRecords_shouldReturnPagedRecords() throws Exception {
        OtaRecordVO record = new OtaRecordVO();
        record.setId(1L);
        record.setState("UPGRADING");
        when(firmwareService.listOtaRecords(eq(1L), eq(20L)))
                .thenReturn(PageUtils.of(List.of(record), 1, 1, 20));

        mockMvc.perform(get("/api/iot/firmware/ota-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].state").value("UPGRADING"))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
