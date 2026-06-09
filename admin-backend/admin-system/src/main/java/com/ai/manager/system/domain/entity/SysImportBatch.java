package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_import_batch")
public class SysImportBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchNo;

    private Long profileId;

    private String bizType;

    /** JSON e.g. {"shopId":1} */
    private String bizContext;

    private String fileName;

    private String filePath;

    /** JSON array of column names detected in file */
    private String detectedColumns;

    private String source;

    private String status;

    private Integer totalRows;

    private Integer successRows;

    private Integer failedRows;

    private Integer unmatchedRows;

    private String errorSummary;

    private String operator;

    private LocalDateTime committedTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
