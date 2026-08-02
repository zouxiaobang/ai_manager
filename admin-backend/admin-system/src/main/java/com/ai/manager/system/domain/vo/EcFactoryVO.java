package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 电商工厂响应 VO
 * 对外暴露工厂信息，剔除逻辑删除标记 deleted 等内部字段，避免实体直接泄露到前端。
 */
@Data
public class EcFactoryVO {

    private Long id;

    private String name;

    /** PRODUCTION / CUSTOMER / CARTON */
    private String factoryType;

    private String contactName;

    private String contactPhone;

    private String address;

    private String remark;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
