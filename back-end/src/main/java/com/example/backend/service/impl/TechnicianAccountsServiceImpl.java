package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.TechnicianAccounts;
import com.example.backend.service.TechnicianAccountsService;
import com.example.backend.mapper.TechnicianAccountsMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【technician_accounts(师傅账号表)】的数据库操作Service实现
* @createDate 2026-03-03 11:26:16
*/
@Service
public class TechnicianAccountsServiceImpl extends ServiceImpl<TechnicianAccountsMapper, TechnicianAccounts>
    implements TechnicianAccountsService{

}




