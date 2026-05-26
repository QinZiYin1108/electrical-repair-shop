package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.SystemMessages;
import com.example.backend.service.SystemMessagesService;
import com.example.backend.mapper.SystemMessagesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【system_messages(系统消息表（站内通知）)】的数据库操作Service实现
* @createDate 2026-03-07 15:43:35
*/
@Service
public class SystemMessagesServiceImpl extends ServiceImpl<SystemMessagesMapper, SystemMessages>
    implements SystemMessagesService{

}




