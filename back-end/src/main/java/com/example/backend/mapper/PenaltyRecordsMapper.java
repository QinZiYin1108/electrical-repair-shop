package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.entity.PenaltyRecords;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【penalty_records(处罚记录表)】的数据库操作Mapper
 */
@Mapper
public interface PenaltyRecordsMapper extends BaseMapper<PenaltyRecords> {}
