package com.ai.manager.system.domain.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EcCompanyInfoSaveRequest {

    @Size(max = 128)
    private String companyName;

    @Size(max = 512)
    private String address;

    @Size(max = 64)
    private String tel;

    @Size(max = 64)
    private String contactName;

    @Size(max = 64)
    private String contactPhone;
}
