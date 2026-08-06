package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.domain.dto.DeviceActivateRequest;
import com.ai.manager.system.iot.domain.dto.DeviceActivateResult;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.dto.OtaCheckRequest;
import com.ai.manager.system.iot.domain.dto.OtaCheckResponse;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotFirmware;
import com.ai.manager.system.iot.domain.entity.IotOtaRecord;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotOtaRecordMapper;
import com.ai.manager.system.iot.service.FirmwareService;
import com.ai.manager.system.iot.service.OtaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtaServiceImpl implements OtaService {

    private final IotDeviceMapper iotDeviceMapper;

    private final IotOtaRecordMapper iotOtaRecordMapper;

    private final FirmwareService firmwareService;

    private final IotProperties iotProperties;

    @Override
    @Transactional
    public OtaCheckResponse check(OtaCheckRequest request) {
        String mac = normalizeMac(request.getMac());
        IotDevice device = iotDeviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .eq(IotDevice::getMac, mac)
                .last("LIMIT 1"));
        if (device == null) {
            device = new IotDevice();
            device.setMac(mac);
            device.setUuid(StringUtils.hasText(request.getUuid())
                    ? request.getUuid() : UUID.randomUUID().toString().replace("-", ""));
            device.setClientId(request.getClientId());
            device.setModel(request.getModel());
            device.setChip(request.getChip());
            device.setFirmwareVersion(request.getFirmwareVersion());
            device.setWsToken(randomToken());
            device.setStatus("UNBOUND");
            device.setOtaState("IDLE");
            device.setDeleted(0);
            iotDeviceMapper.insert(device);
        } else {
            if (request.getFirmwareVersion() != null) {
                device.setFirmwareVersion(request.getFirmwareVersion());
            }
            if (request.getModel() != null) {
                device.setModel(request.getModel());
            }
            device.setLastSeenAt(LocalDateTime.now());
            iotDeviceMapper.updateById(device);
        }

        OtaCheckResponse resp = new OtaCheckResponse();
        resp.setWebsocket(new OtaCheckResponse.WebsocketConfig(
                iotProperties.getWsUrl(), device.getWsToken(), iotProperties.getProtocolVersion()));
        resp.setMqtt(new OtaCheckResponse.MqttConfig(
                iotProperties.getMqttEndpoint(),
                iotProperties.getMqttClientIdPrefix() + device.getMac(),
                iotProperties.getMqttUsername(),
                iotProperties.getMqttPassword()));
        resp.setServerTime(new OtaCheckResponse.ServerTime(
                System.currentTimeMillis() / 1000, iotProperties.getTimezoneOffsetMinutes() * 60));

        IotFirmware latest = firmwareService.latestPublished();
        if (latest != null && shouldUpgrade(latest, device.getFirmwareVersion())) {
            resp.setFirmware(new OtaCheckResponse.FirmwareInfo(
                    latest.getVersion(),
                    iotProperties.getOtaBaseUrl() + "/" + latest.getId(),
                    latest.getForceUpgrade() != null && latest.getForceUpgrade() == 1));
            recordOta(device.getId(), latest.getId());
        }
        return resp;
    }

    @Override
    @Transactional
    public DeviceActivateResult activate(DeviceActivateRequest request) {
        String mac = normalizeMac(request.getMac());
        IotDevice device = iotDeviceMapper.selectOne(new LambdaQueryWrapper<IotDevice>()
                .eq(IotDevice::getDeleted, 0)
                .eq(IotDevice::getMac, mac)
                .last("LIMIT 1"));
        if (device == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "设备未注册");
        }
        String expected = hmacSha256Hex(iotProperties.getActivationSecret(),
                mac + ":" + request.getNonce() + ":" + request.getTimestamp());
        if (!expected.equalsIgnoreCase(request.getSignature())) {
            log.warn("设备激活签名校验失败 mac={}", mac);
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "激活签名校验失败");
        }
        device.setStatus("ACTIVATED");
        device.setActivatedAt(LocalDateTime.now());
        device.setLastSeenAt(LocalDateTime.now());
        iotDeviceMapper.updateById(device);

        DeviceActivateResult result = new DeviceActivateResult();
        result.setSuccess(true);
        result.setDeviceId(device.getId());
        result.setActivatedAt(device.getActivatedAt());
        return result;
    }

    @Override
    public FirmwareDownloadInfo getDownloadInfo(Long firmwareId) {
        return firmwareService.downloadInfo(firmwareId);
    }

    /** 是否需要升级：设备版本为空、强制升级、或服务器版本更新。 */
    private boolean shouldUpgrade(IotFirmware latest, String currentVersion) {
        if (latest.getForceUpgrade() != null && latest.getForceUpgrade() == 1) {
            return true;
        }
        if (!StringUtils.hasText(currentVersion)) {
            return true;
        }
        return compareVersion(latest.getVersion(), currentVersion) > 0;
    }

    /** 点分版本号比较：返回正数表示 a 更新。 */
    private int compareVersion(String a, String b) {
        String[] ap = a.split("\\.");
        String[] bp = b.split("\\.");
        int len = Math.max(ap.length, bp.length);
        for (int i = 0; i < len; i++) {
            int av = i < ap.length ? parseInt(ap[i]) : 0;
            int bv = i < bp.length ? parseInt(bp[i]) : 0;
            if (av != bv) {
                return Integer.compare(av, bv);
            }
        }
        return 0;
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void recordOta(Long deviceId, Long firmwareId) {
        IotOtaRecord record = new IotOtaRecord();
        record.setDeviceId(deviceId);
        record.setFirmwareId(firmwareId);
        record.setState("UPGRADING");
        record.setProgress(0);
        record.setStartedAt(LocalDateTime.now());
        record.setDeleted(0);
        iotOtaRecordMapper.insert(record);
    }

    private String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String normalizeMac(String mac) {
        return mac == null ? "" : mac.toLowerCase().replace(":", "").trim();
    }
}
