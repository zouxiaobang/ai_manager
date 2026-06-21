package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("nb_baidu_pan_auth")
public class NbBaiduPanAuth {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime expiresAt;

    private Long baiduUid;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
