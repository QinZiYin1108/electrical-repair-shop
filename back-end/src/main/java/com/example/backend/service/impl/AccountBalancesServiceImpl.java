package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.AccountBalances;
import com.example.backend.service.AccountBalancesService;
import com.example.backend.mapper.AccountBalancesMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【account_balances(账户余额表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:15
*/
@Service
public class AccountBalancesServiceImpl extends ServiceImpl<AccountBalancesMapper, AccountBalances>
    implements AccountBalancesService{

}




