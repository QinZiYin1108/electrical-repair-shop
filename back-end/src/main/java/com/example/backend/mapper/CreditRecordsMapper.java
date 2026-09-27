package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.CreditRecords;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【credit_records(信用积分记录表)】的数据库操作Mapper
 */
@Mapper
public interface CreditRecordsMapper extends BaseMapper<CreditRecords> {}
