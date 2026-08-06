package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;
import com.ai.manager.system.iot.service.FirmwareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 固件管理接口（后台鉴权）。
 */
@RestController
@RequestMapping("/api/iot/firmware")
@RequiredArgsConstructor
public class FirmwareController {

    private final FirmwareService firmwareService;

    @PostMapping("/upload")
    public ApiResult<FirmwareVO> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam String version,
                                        @RequestParam(required = false) String releaseNote,
                                        @RequestParam(required = false, defaultValue = "0") Integer force) throws IOException {
        return ApiResult.ok(firmwareService.upload(file.getBytes(), version, releaseNote, force));
    }

    @PostMapping("/{id}/publish")
    public ApiResult<FirmwareVO> publish(@PathVariable Long id) {
        return ApiResult.ok(firmwareService.publish(id));
    }

    @GetMapping
    public ApiResult<List<FirmwareVO>> list() {
        return ApiResult.ok(firmwareService.listFirmwares());
    }

    @GetMapping("/{id}")
    public ApiResult<FirmwareVO> get(@PathVariable Long id) {
        return ApiResult.ok(firmwareService.getFirmware(id));
    }

    @GetMapping("/ota-records")
    public ApiResult<List<OtaRecordVO>> otaRecords() {
        return ApiResult.ok(firmwareService.listOtaRecords());
    }
}
