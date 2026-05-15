package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ConversationMessages;
import com.example.backend.service.ConversationMessagesService;
import com.example.backend.mapper.ConversationMessagesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【conversation_messages(站内消息表（用户与师傅）)】的数据库操作Service实现
* @createDate 2026-03-06 17:48:10
*/
@Service
public class ConversationMessagesServiceImpl extends ServiceImpl<ConversationMessagesMapper, ConversationMessages>
    implements ConversationMessagesService{

}




