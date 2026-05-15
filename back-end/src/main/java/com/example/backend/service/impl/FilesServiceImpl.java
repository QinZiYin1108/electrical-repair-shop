package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.Files;
import com.example.backend.service.FilesService;
import com.example.backend.mapper.FilesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【files(文件表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class FilesServiceImpl extends ServiceImpl<FilesMapper, Files>
    implements FilesService{

}




