package com.ai.manager.system.mapper;

import com.ai.manager.system.domain.entity.PixelDogState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PixelDogStateMapper extends BaseMapper<PixelDogState> {

    List<PixelDogState> selectActiveState();
}