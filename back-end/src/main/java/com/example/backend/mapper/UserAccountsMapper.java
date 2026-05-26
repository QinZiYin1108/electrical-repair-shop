package com.example.backend.mapper;

import com.example.backend.entity.UserAccounts;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
* @author Administrator
* @description 针对表【user_accounts(用户账号表)】的数据库操作Mapper
* @createDate 2026-03-03 11:26:16
* @Entity com.example.backend.entity.UserAccounts
*/
public interface UserAccountsMapper extends BaseMapper<UserAccounts> {

    UserAccounts selectByEmailIncludeDeleted(@Param("email") String email);

    UserAccounts selectByWxOpenidIncludeDeleted(@Param("wxOpenid") String wxOpenid);
}




