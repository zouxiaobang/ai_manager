package com.ai.manager.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class EcDeliveryNoteConfigVO {

    private String title;

    private String address;

    private String tel;

    private String preparedBy;

    private String shipFromName;

    private String shipFromPhone;

    private String shipFromAddress;

    private List<String> requirementItems = new ArrayList<>();

    private List<String> noteItems = new ArrayList<>();

    private LocalDateTime updateTime;
}
