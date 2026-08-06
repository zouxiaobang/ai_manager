package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.domain.dto.DeviceBindRequest;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.vo.DeviceVO;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.service.DeviceService;
import com.ai.manager.system.iot.websocket.DeviceWsHandler;
import com.ai.manager.system.iot.websocket.WsSessionRegistry;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final IotDeviceMapper iotDeviceMapper;

    private final WsSessionRegistry wsSessionRegistry;

    private final DeviceWsHandler deviceWsHandler;

    @Override
    public List<DeviceVO> listDevices() {
        List<IotDevice> list = iotDeviceMapper.selectList(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .orderByDesc(IotDevice::getId));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public DeviceVO getDevice(Long id) {
        IotDevice device = iotDeviceMapper.selectById(id);
        if (device == null || device.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "设备不存在");
        }
        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO bind(DeviceBindRequest request) {
        String mac = normalizeMac(request.getMac());
        IotDevice device = iotDeviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .eq(IotDevice::getMac, mac)
                .last("LIMIT 1"));
        if (device == null) {
            device = new IotDevice();
            device.setMac(mac);
            device.setUuid(StringUtils.hasText(request.getUuid()) ? request.getUuid() : UUID.randomUUID().toString().replace("-", ""));
            device.setWsToken(randomToken());
            device.setStatus("BOUND");
            device.setOtaState("IDLE");
            device.setDeleted(0);
        }
        if (request.getModel() != null) {
            device.setModel(request.getModel());
        }
        if (request.getChip() != null) {
            device.setChip(request.getChip());
        }
        if (request.getFirmwareVersion() != null) {
            device.setFirmwareVersion(request.getFirmwareVersion());
        }
        if (device.getId() == null) {
            iotDeviceMapper.insert(device);
        } else {
            iotDeviceMapper.updateById(device);
        }
        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO updateStatus(Long id, String status) {
        IotDevice device = requireDevice(id);
        device.setStatus(status);
        iotDeviceMapper.updateById(device);
        return toVO(device);
    }

    @Override
    @Transactional
    public DeviceVO reboot(Long id) {
        IotDevice device = requireDevice(id);
        if (wsSessionRegistry.isOnline(device.getMac())) {
            deviceWsHandler.sendSystem(device.getMac(), "reboot");
            log.info("已下发 reboot 命令 deviceId={}, mac={}", device.getId(), device.getMac());
        } else {
            log.info("设备离线，跳过 reboot 下发 deviceId={}", device.getId());
        }
        return toVO(device);
    }

    @Override
    public IotDevice findByMac(String mac) {
        if (!StringUtils.hasText(mac)) {
            return null;
        }
        return iotDeviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .eq(IotDevice::getMac, normalizeMac(mac))
                .last("LIMIT 1"));
    }

    private IotDevice requireDevice(Long id) {
        IotDevice device = iotDeviceMapper.selectById(id);
        if (device == null || device.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "设备不存在");
        }
        return device;
    }

    private DeviceVO toVO(IotDevice d) {
        DeviceVO vo = new DeviceVO();
        vo.setId(d.getId());
        vo.setUuid(d.getUuid());
        vo.setClientId(d.getClientId());
        vo.setMac(d.getMac());
        vo.setModel(d.getModel());
        vo.setChip(d.getChip());
        vo.setFirmwareVersion(d.getFirmwareVersion());
        vo.setActivatedAt(d.getActivatedAt());
        vo.setLastSeenAt(d.getLastSeenAt());
        vo.setStatus(d.getStatus());
        vo.setSessionId(d.getSessionId());
        vo.setOtaState(d.getOtaState());
        vo.setOnline(wsSessionRegistry.isOnline(d.getMac()));
        vo.setCreateTime(d.getCreateTime());
        vo.setUpdateTime(d.getUpdateTime());
        return vo;
    }

    private String normalizeMac(String mac) {
        return mac == null ? "" : mac.toLowerCase().replace(":", "").trim();
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
