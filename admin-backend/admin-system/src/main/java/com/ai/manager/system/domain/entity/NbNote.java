package com.ai.manager.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("nb_note")
public class NbNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long notebookId;

    private String title;

    private String storageType;

    private String storagePath;

    private Long storageFsId;

    private String contentHash;

    private Long contentSize;

    private Integer contentVersion;

    private String contentExcerpt;

    private String syncStatus;

    private String syncError;

    private String noteType;

    private Integer isPinned;

    private Integer isFavorite;

    private Integer sortOrder;

    private String status;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
