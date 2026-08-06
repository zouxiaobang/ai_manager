package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;
import com.ai.manager.system.iot.service.FirmwareService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    /** 强制升级：置 force=1 并发布，下次 OTA check 对在线设备强制下发。 */
    @PostMapping("/{id}/force")
    public ApiResult<FirmwareVO> forceUpgrade(@PathVariable Long id) {
        return ApiResult.ok(firmwareService.forceUpgrade(id));
    }

    /** 删除固件（已发布禁止删除）。 */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        firmwareService.delete(id);
        return ApiResult.ok();
    }

    @GetMapping
    public ApiResult<PageResult<FirmwareVO>> list(@RequestParam(defaultValue = "1") Long page,
                                                  @RequestParam(defaultValue = "20") Long pageSize,
                                                  @RequestParam(required = false) String keyword) {
        return ApiResult.ok(firmwareService.listFirmwares(page, pageSize, keyword));
    }

    @GetMapping("/{id}")
    public ApiResult<FirmwareVO> get(@PathVariable Long id) {
        return ApiResult.ok(firmwareService.getFirmware(id));
    }

    @GetMapping("/ota-records")
    public ApiResult<PageResult<OtaRecordVO>> otaRecords(@RequestParam(defaultValue = "1") Long page,
                                                         @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResult.ok(firmwareService.listOtaRecords(page, pageSize));
    }
}
