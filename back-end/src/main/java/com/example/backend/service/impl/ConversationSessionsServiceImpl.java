package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ConversationSessions;
import com.example.backend.service.ConversationSessionsService;
import com.example.backend.mapper.ConversationSessionsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【conversation_sessions(站内会话表（用户与师傅）)】的数据库操作Service实现
* @createDate 2026-03-06 17:48:10
*/
@Service
public class ConversationSessionsServiceImpl extends ServiceImpl<ConversationSessionsMapper, ConversationSessions>
    implements ConversationSessionsService{

}




