package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EcCompanyInfoVO {

    private String companyName;

    private String address;

    private String tel;

    private String contactName;

    private String contactPhone;

    private LocalDateTime updateTime;
}
