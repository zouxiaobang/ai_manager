package com.ai.manager.system.iot.mapper;

import com.ai.manager.system.iot.domain.entity.IotDevice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IotDeviceMapper extends BaseMapper<IotDevice> {
}
