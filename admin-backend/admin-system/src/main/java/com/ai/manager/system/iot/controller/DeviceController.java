package com.ai.manager.system.iot.controller;

import com.ai.manager.common.result.ApiResult;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.system.iot.domain.dto.DeviceBindRequest;
import com.ai.manager.system.iot.domain.dto.DeviceUpdateRequest;
import com.ai.manager.system.iot.domain.vo.DeviceOnlineStatusVO;
import com.ai.manager.system.iot.domain.vo.DeviceVO;
import com.ai.manager.system.iot.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备管理接口（后台鉴权）。
 */
@RestController
@RequestMapping("/api/iot/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ApiResult<PageResult<DeviceVO>> list(@RequestParam(defaultValue = "1") Long page,
                                                @RequestParam(defaultValue = "20") Long pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status) {
        return ApiResult.ok(deviceService.listDevices(page, pageSize, keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResult<DeviceVO> get(@PathVariable Long id) {
        return ApiResult.ok(deviceService.getDevice(id));
    }

    /** 探测设备在线状态（后台「测在线」按钮）。 */
    @GetMapping("/{id}/online")
    public ApiResult<DeviceOnlineStatusVO> online(@PathVariable Long id) {
        return ApiResult.ok(deviceService.probeOnline(id));
    }

    @PostMapping("/bind")
    public ApiResult<DeviceVO> bind(@Valid @RequestBody DeviceBindRequest request) {
        return ApiResult.ok(deviceService.bind(request));
    }

    @PutMapping("/{id}")
    public ApiResult<DeviceVO> update(@PathVariable Long id, @RequestBody DeviceUpdateRequest request) {
        return ApiResult.ok(deviceService.update(id, request));
    }

    @PutMapping("/{id}/status")
    public ApiResult<DeviceVO> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResult.ok(deviceService.updateStatus(id, status));
    }

    @PostMapping("/{id}/reboot")
    public ApiResult<DeviceVO> reboot(@PathVariable Long id) {
        return ApiResult.ok(deviceService.reboot(id));
    }
}
