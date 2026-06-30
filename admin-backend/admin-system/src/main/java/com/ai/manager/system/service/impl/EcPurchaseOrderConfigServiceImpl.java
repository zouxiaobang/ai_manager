package com.ai.manager.system.service.impl;

import com.ai.manager.common.exception.BusinessException;
import com.ai.manager.common.result.ResultCode;
import com.ai.manager.system.domain.dto.EcPurchaseOrderConfigSaveRequest;
import com.ai.manager.system.domain.entity.EcPurchaseOrderConfig;
import com.ai.manager.system.domain.vo.EcPurchaseOrderConfigVO;
import com.ai.manager.system.mapper.EcPurchaseOrderConfigMapper;
import com.ai.manager.system.service.EcPurchaseOrderConfigService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcPurchaseOrderConfigServiceImpl extends ServiceImpl<EcPurchaseOrderConfigMapper, EcPurchaseOrderConfig>
    implements EcPurchaseOrderConfigService {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  @Override
  public EcPurchaseOrderConfigVO getConfig() {
    EcPurchaseOrderConfig config = getOrCreateSingleton();
    return toVO(config);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public EcPurchaseOrderConfigVO saveConfig(EcPurchaseOrderConfigSaveRequest request) {
    if (request == null) {
      throw new BusinessException(ResultCode.BAD_REQUEST);
    }
    if (!StringUtils.hasText(request.getTitle())) {
      throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "采购单标题不能为空");
    }
    EcPurchaseOrderConfig config = getOrCreateSingleton();
    config.setTitle(request.getTitle().trim());
    config.setAddress(trimToNull(request.getAddress()));
    config.setTel(trimToNull(request.getTel()));
    config.setRequirementItems(writeJsonList(request.getRequirementItems()));
    config.setNoteItems(writeJsonList(request.getNoteItems()));
    config.setPreparedBy(trimToNull(request.getPreparedBy()));
    config.setPreparedPhone(trimToNull(request.getPreparedPhone()));
    config.setReceiverName(trimToNull(request.getReceiverName()));
    config.setReceiverPhone(trimToNull(request.getReceiverPhone()));
    config.setReceiverAddress(trimToNull(request.getReceiverAddress()));
    config.setCompanyNo(trimToNull(request.getCompanyNo()));
    updateById(config);
    return toVO(getById(EcPurchaseOrderConfig.SINGLETON_ID));
  }

  private EcPurchaseOrderConfig getOrCreateSingleton() {
    EcPurchaseOrderConfig config = getById(EcPurchaseOrderConfig.SINGLETON_ID);
    if (config != null) {
      return config;
    }
    config = defaultConfig();
    save(config);
    return config;
  }

  private EcPurchaseOrderConfig defaultConfig() {
    EcPurchaseOrderConfig config = new EcPurchaseOrderConfig();
    config.setId(EcPurchaseOrderConfig.SINGLETON_ID);
    config.setTitle("唯十嘉采购单");
    config.setAddress("地址：汕头市澄海区莲下镇东湾文化公园");
    config.setTel("TEL：18819446360");
    config.setRequirementItems(writeJsonList(List.of(
        "出货时间、日期不能再拖后！产品必须通过美国站全检测以及CPSIA，CPC检测。",
        "大货产品颜色必须跟左上角图片一样，不能更改。",
        "按新纸箱规格包装，确保货物稳固，运输过程中不会破损。")));
    config.setNoteItems(writeJsonList(List.of(
        "本单为采购合同，请工厂签字盖章回传，并妥善保管。",
        "产品名称必须与采购单一致，如有变更请提前沟通确认。",
        "请按约定时间交货，如有延误请第一时间通知我司。",
        "货款与发货事宜请与采购联系人核对后执行。")));
    config.setPreparedBy("张小姐");
    config.setPreparedPhone("18819446360");
    config.setReceiverName("张小姐");
    config.setReceiverPhone("18819446360");
    config.setReceiverAddress("见上面地址");
    config.setCompanyNo("");
    return config;
  }

  private EcPurchaseOrderConfigVO toVO(EcPurchaseOrderConfig config) {
    EcPurchaseOrderConfigVO vo = new EcPurchaseOrderConfigVO();
    vo.setTitle(config.getTitle());
    vo.setAddress(config.getAddress());
    vo.setTel(config.getTel());
    vo.setRequirementItems(readJsonList(config.getRequirementItems()));
    vo.setNoteItems(readJsonList(config.getNoteItems()));
    vo.setPreparedBy(config.getPreparedBy());
    vo.setPreparedPhone(config.getPreparedPhone());
    vo.setReceiverName(config.getReceiverName());
    vo.setReceiverPhone(config.getReceiverPhone());
    vo.setReceiverAddress(config.getReceiverAddress());
    vo.setCompanyNo(config.getCompanyNo());
    vo.setUpdateTime(config.getUpdateTime());
    return vo;
  }

  private List<String> readJsonList(String json) {
    if (!StringUtils.hasText(json)) {
      return new ArrayList<>();
    }
    try {
      List<String> items = objectMapper.readValue(json, STRING_LIST);
      return items != null ? items : new ArrayList<>();
    } catch (JsonProcessingException ex) {
      return new ArrayList<>();
    }
  }

  private String writeJsonList(List<String> items) {
    List<String> normalized = new ArrayList<>();
    if (items != null) {
      for (String item : items) {
        if (StringUtils.hasText(item)) {
          normalized.add(item.trim());
        }
      }
    }
    try {
      return objectMapper.writeValueAsString(normalized);
    } catch (JsonProcessingException ex) {
      throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "列表序列化失败");
    }
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }
}
