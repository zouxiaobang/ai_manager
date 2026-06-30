package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_system_config")
public class EcSystemConfig {

    @TableId
    private String configKey;

    private String configJson;

    private LocalDateTime updateTime;
}
