package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.SystemMessages;
import com.example.backend.mapper.SystemMessagesMapper;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;

/**
 * @author Administrator
 * @description 针对表【system_messages(系统消息表（站内通知）)】的数据库操作Service实现
 * @createDate 2026-03-07 15:43:35
 */
@Service
public class SystemMessagesServiceImpl extends ServiceImpl<SystemMessagesMapper, SystemMessages>
        implements SystemMessagesService {

    @Override
    public SystemMessages createSystemMessage(
            String receiverId,
            Integer receiverType,
            String title,
            String content,
            Integer messageType,
            String businessType,
            String businessId,
            Integer priority) {
        long now = System.currentTimeMillis();
        SystemMessages message = new SystemMessages();
        message.setId(SnowflakeIdUtil.nextSystemMessageId());
        message.setReceiverId(receiverId);
        message.setReceiverType(receiverType);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setBusinessType(businessType);
        message.setBusinessId(businessId);
        message.setPriority(priority);
        message.setIsRead(0);
        message.setCreatedTime(now);
        message.setUpdatedTime(now);
        message.setVersion(0);
        message.setIsDelete(0);
        save(message);
        return message;
    }
}
