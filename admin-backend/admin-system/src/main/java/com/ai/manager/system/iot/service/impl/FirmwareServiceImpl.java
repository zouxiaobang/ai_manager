package com.ai.manager.system.iot.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.PageResult;
import com.ai.manager.common.result.PageUtils;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.iot.config.IotProperties;
import com.ai.manager.system.iot.domain.dto.FirmwareDownloadInfo;
import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.ai.manager.system.iot.domain.entity.IotFirmware;
import com.ai.manager.system.iot.domain.entity.IotOtaRecord;
import com.ai.manager.system.iot.domain.vo.FirmwareVO;
import com.ai.manager.system.iot.domain.vo.OtaRecordVO;
import com.ai.manager.system.iot.mapper.IotDeviceMapper;
import com.ai.manager.system.iot.mapper.IotFirmwareMapper;
import com.ai.manager.system.iot.mapper.IotOtaRecordMapper;
import com.ai.manager.system.iot.service.FirmwareService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FirmwareServiceImpl implements FirmwareService {

    private static final String PUBLISHED = "PUBLISHED";
    private static final String DRAFT = "DRAFT";

    private final IotFirmwareMapper iotFirmwareMapper;

    private final IotOtaRecordMapper iotOtaRecordMapper;

    private final IotDeviceMapper iotDeviceMapper;

    private final IotProperties iotProperties;

    @Override
    @Transactional
    public FirmwareVO upload(byte[] content, String version, String releaseNote, Integer force) {
        if (content == null || content.length == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "固件文件不能为空");
        }
        if (!StringUtils.hasText(version)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "固件版本不能为空");
        }
        String fileHash = sha256Hex(content);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".bin";
        Path dir = Paths.get(iotProperties.getOtaDir()).toAbsolutePath().normalize();
        Path target = dir.resolve(fileName);
        try {
            Files.createDirectories(dir);
            Files.write(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.error("保存固件文件失败", e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "保存固件文件失败");
        }

        IotFirmware firmware = new IotFirmware();
        firmware.setVersion(version.trim());
        firmware.setFilePath(target.toString());
        firmware.setFileHash(fileHash);
        firmware.setSize((long) content.length);
        firmware.setForceUpgrade(force != null && force == 1 ? 1 : 0);
        firmware.setStatus(DRAFT);
        firmware.setReleaseNote(releaseNote);
        firmware.setDeleted(0);
        iotFirmwareMapper.insert(firmware);
        return toVO(firmware);
    }

    @Override
    @Transactional
    public FirmwareVO publish(Long id) {
        IotFirmware firmware = requireFirmware(id);
        firmware.setStatus(PUBLISHED);
        iotFirmwareMapper.updateById(firmware);
        return toVO(firmware);
    }

    @Override
    @Transactional
    public FirmwareVO forceUpgrade(Long id) {
        IotFirmware firmware = requireFirmware(id);
        // force=1 使 OtaServiceImpl.check 对同版本设备也强制下发
        firmware.setForceUpgrade(1);
        firmware.setStatus(PUBLISHED);
        iotFirmwareMapper.updateById(firmware);
        return toVO(firmware);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        IotFirmware firmware = requireFirmware(id);
        if (PUBLISHED.equals(firmware.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "已发布固件不可删除，请先取消发布");
        }
        // 清理文件尽力而为，失败仅告警不阻塞（记录仍可查）
        if (StringUtils.hasText(firmware.getFilePath())) {
            try {
                Files.deleteIfExists(Paths.get(firmware.getFilePath()));
            } catch (IOException e) {
                log.warn("删除固件文件失败 firmwareId={}, path={}", id, firmware.getFilePath(), e);
            }
        }
        iotFirmwareMapper.deleteById(id);
    }

    @Override
    public PageResult<FirmwareVO> listFirmwares(Long page, Long pageSize, String keyword) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        Page<IotFirmware> entityPage = iotFirmwareMapper.selectPage(new Page<>(p, ps),
                new LambdaQueryWrapper<IotFirmware>()
                        .eq(IotFirmware::getDeleted, 0)
                        .and(StringUtils.hasText(keyword), w -> w.like(IotFirmware::getVersion, keyword)
                                .or().like(IotFirmware::getReleaseNote, keyword))
                        .orderByDesc(IotFirmware::getId));
        List<FirmwareVO> vos = entityPage.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageUtils.of(vos, entityPage.getTotal(), p, ps);
    }

    @Override
    public FirmwareVO getFirmware(Long id) {
        return toVO(requireFirmware(id));
    }

    @Override
    public IotFirmware latestPublished() {
        return iotFirmwareMapper.selectOne(new LambdaQueryWrapper<IotFirmware>()
                .eq(IotFirmware::getDeleted, 0)
                .eq(IotFirmware::getStatus, PUBLISHED)
                .orderByDesc(IotFirmware::getId)
                .last("LIMIT 1"));
    }

    @Override
    public FirmwareDownloadInfo downloadInfo(Long firmwareId) {
        IotFirmware firmware = requireFirmware(firmwareId);
        Path target = Paths.get(firmware.getFilePath());
        try {
            byte[] bytes = Files.readAllBytes(target);
            return new FirmwareDownloadInfo(firmware.getVersion(), firmware.getFileHash(), bytes.length, bytes);
        } catch (IOException e) {
            log.error("读取固件文件失败 firmwareId={}, path={}", firmwareId, firmware.getFilePath(), e);
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "固件文件不存在");
        }
    }

    @Override
    public PageResult<OtaRecordVO> listOtaRecords(Long page, Long pageSize) {
        long p = PageUtils.normalizePage(page);
        long ps = PageUtils.normalizePageSize(pageSize);
        Page<IotOtaRecord> entityPage = iotOtaRecordMapper.selectPage(new Page<>(p, ps),
                new LambdaQueryWrapper<IotOtaRecord>()
                        .eq(IotOtaRecord::getDeleted, 0)
                        .orderByDesc(IotOtaRecord::getId));
        List<OtaRecordVO> vos = entityPage.getRecords().stream().map(r -> {
            OtaRecordVO vo = new OtaRecordVO();
            vo.setId(r.getId());
            vo.setDeviceId(r.getDeviceId());
            vo.setFirmwareId(r.getFirmwareId());
            vo.setState(r.getState());
            vo.setProgress(r.getProgress());
            vo.setStartedAt(r.getStartedAt());
            vo.setFinishedAt(r.getFinishedAt());
            vo.setCreateTime(r.getCreateTime());
            if (r.getDeviceId() != null) {
                IotDevice device = iotDeviceMapper.selectById(r.getDeviceId());
                if (device != null) {
                    vo.setDeviceName(device.getMac());
                }
            }
            if (r.getFirmwareId() != null) {
                IotFirmware fw = iotFirmwareMapper.selectById(r.getFirmwareId());
                if (fw != null) {
                    vo.setFirmwareVersion(fw.getVersion());
                }
            }
            return vo;
        }).collect(Collectors.toList());
        return PageUtils.of(vos, entityPage.getTotal(), p, ps);
    }

    private IotFirmware requireFirmware(Long id) {
        IotFirmware firmware = iotFirmwareMapper.selectById(id);
        if (firmware == null || firmware.getDeleted() == 1) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "固件不存在");
        }
        return firmware;
    }

    private FirmwareVO toVO(IotFirmware f) {
        FirmwareVO vo = new FirmwareVO();
        vo.setId(f.getId());
        vo.setVersion(f.getVersion());
        vo.setFileHash(f.getFileHash());
        vo.setSize(f.getSize());
        vo.setForce(f.getForceUpgrade());
        vo.setStatus(f.getStatus());
        vo.setReleaseNote(f.getReleaseNote());
        vo.setCreateTime(f.getCreateTime());
        vo.setUpdateTime(f.getUpdateTime());
        return vo;
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
