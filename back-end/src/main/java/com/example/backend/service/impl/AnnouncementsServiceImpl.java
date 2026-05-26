package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.Announcements;
import com.example.backend.mapper.AnnouncementsMapper;
import com.example.backend.service.AnnouncementsService;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementsServiceImpl extends ServiceImpl<AnnouncementsMapper, Announcements>
    implements AnnouncementsService {
}

