package com.example.backend.mapper;

import com.example.backend.entity.TechnicianAccounts;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author Administrator
* @description 针对表【technician_accounts(师傅账号表)】的数据库操作Mapper
* @createDate 2026-03-03 11:26:16
* @Entity com.example.backend.entity.TechnicianAccounts
*/
public interface TechnicianAccountsMapper extends BaseMapper<TechnicianAccounts> {

    /**
     * 按邮箱查询账号（包含逻辑删除数据）
     * 用于登录场景：避免已注销账号因逻辑删除过滤而被误判为“不存在”，进而触发重复注册/唯一键冲突。
     */
    TechnicianAccounts selectByEmailIncludeDeleted(@Param("email") String email);
}




