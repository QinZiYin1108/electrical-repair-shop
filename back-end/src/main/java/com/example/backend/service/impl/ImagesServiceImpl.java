package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.Images;
import com.example.backend.service.ImagesService;
import com.example.backend.mapper.ImagesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【images(图片表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class ImagesServiceImpl extends ServiceImpl<ImagesMapper, Images>
    implements ImagesService{

}




