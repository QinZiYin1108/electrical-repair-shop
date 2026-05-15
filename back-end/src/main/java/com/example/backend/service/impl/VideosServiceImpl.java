package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.Videos;
import com.example.backend.service.VideosService;
import com.example.backend.mapper.VideosMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【videos(视频表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class VideosServiceImpl extends ServiceImpl<VideosMapper, Videos>
    implements VideosService{

}




