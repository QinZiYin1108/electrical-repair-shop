package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("announcements")
public class Announcements {

    @TableId
    private String id;

    /**
     * 1-banner, 2-notice
     */
    private Integer channel;

    /**
     * 1-image, 2-text
     */
    private Integer contentType;

    private String title;

    private String subtitle;

    private String content;

    private String emoji;

    private Integer isActive;

    private Integer sortOrder;

    private Long startTime;

    private Long endTime;

    private Long createdTime;

    private Long updatedTime;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDelete;
}

