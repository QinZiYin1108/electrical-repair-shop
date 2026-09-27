package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.Reports;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【reports(举报表)】的数据库操作Mapper
 */
@Mapper
public interface ReportsMapper extends BaseMapper<Reports> {}
