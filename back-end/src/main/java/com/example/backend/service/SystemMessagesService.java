package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.SystemMessages;

/**
 * @author Administrator
 * @description 针对表【system_messages(系统消息表（站内通知）)】的数据库操作Service
 * @createDate 2026-03-07 15:43:35
 */
public interface SystemMessagesService extends IService<SystemMessages> {

    /** 创建一条系统消息（便捷方法） */
    SystemMessages createSystemMessage(
            String receiverId,
            Integer receiverType,
            String title,
            String content,
            Integer messageType,
            String businessType,
            String businessId,
            Integer priority);
}
