package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.system.iot.domain.dto.DeviceActivateRequest;
import com.ai.manager.system.iot.domain.dto.DeviceActivateResult;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.dto.OtaCheckRequest;
import com.ai.manager.system.iot.domain.dto.OtaCheckResponse;
import com.ai.manager.system.iot.domain.dto.OtaStatusRequest;
import com.ai.manager.system.iot.service.OtaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 设备侧 OTA 接口（走设备 token，不依赖后台登录）。
 */
@RestController
@RequestMapping("/api/iot/ota")
@RequiredArgsConstructor
public class DeviceOtaController {

    private final OtaService otaService;

    /** 设备版本检查 + 配置下发。 */
    @PostMapping("/check")
    public ApiResult<OtaCheckResponse> check(@RequestBody OtaCheckRequest request) {
        return ApiResult.ok(otaService.check(request));
    }

    /** 设备激活挑战应答。 */
    @PostMapping("/activate")
    public ApiResult<DeviceActivateResult> activate(@RequestBody DeviceActivateRequest request) {
        return ApiResult.ok(otaService.activate(request));
    }

    /** 固件二进制下载（带哈希与大小响应头）。 */
    @GetMapping("/download/{firmwareId}")
    public ResponseEntity<byte[]> download(@PathVariable Long firmwareId) {
        FirmwareDownloadInfo info = otaService.getDownloadInfo(firmwareId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("firmware-" + info.getVersion() + ".bin", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(info.getSize());
        headers.set("X-Firmware-Version", info.getVersion());
        headers.set("X-Firmware-Hash", info.getFileHash());
        return ResponseEntity.ok().headers(headers).body(info.getContent());
    }

    /** 设备 OTA 升级状态/进度上报（固件下载中每 10%、成功/失败各上报一次）。 */
    @PostMapping("/status")
    public ApiResult<Void> reportStatus(@RequestBody OtaStatusRequest request) {
        otaService.reportStatus(request);
        return ApiResult.ok();
    }
}
