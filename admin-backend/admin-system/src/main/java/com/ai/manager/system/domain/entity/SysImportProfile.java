package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_import_profile")
public class SysImportProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String bizType;

    private Long platformId;

    private String scopeKey;

    private Long shopId;

    private String fileType;

    private Integer headerRow;

    private Integer dataStartRow;

    private String sheetName;

    /** JSON: backend field -> document column name */
    private String columnMapping;

    /** JSON: source value -> target value */
    private String valueMapping;

    /** JSON: biz-specific extras */
    private String extraConfig;

    private Integer enabled;

    private String remark;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
