package com.ai.manager.system.iot.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 固件管理视图对象
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FirmwareVO {

    private Long id;

    private String version;

    private String fileHash;

    private Long size;

    private Integer force;

    private String status;

    private String releaseNote;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
