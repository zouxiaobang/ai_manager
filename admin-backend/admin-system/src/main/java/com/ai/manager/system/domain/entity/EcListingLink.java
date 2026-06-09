package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ec_listing_link")
public class EcListingLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Long shopId;

    /** 平台商品链接 URL */
    private String platformUrl;

    private LocalDateTime listingTime;

    private String remark;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
