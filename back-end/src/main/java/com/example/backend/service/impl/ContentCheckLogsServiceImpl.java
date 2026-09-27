package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.entity.ContentCheckLogs;
import com.example.backend.mapper.ContentCheckLogsMapper;
import com.example.backend.service.ContentCheckLogsService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 内容审核日志 Service 实现。 */
@Service
public class ContentCheckLogsServiceImpl
        extends ServiceImpl<ContentCheckLogsMapper, ContentCheckLogs>
        implements ContentCheckLogsService {

    @Override
    public void logCheck(
            String accountId,
            int accountType,
            int contentType,
            String content,
            int checkResult,
            String hitKeyword,
            String suggestion) {
        ContentCheckLogs log = new ContentCheckLogs();
        log.setId(SnowflakeIdUtil.nextContentCheckLogId());
        log.setAccountId(accountId);
        log.setAccountType(accountType);
        log.setContentType(contentType);
        log.setContent(content);
        log.setCheckResult(checkResult);
        log.setHitKeyword(hitKeyword); // 阿里云 label
        log.setHitRuleId(suggestion); // 复用字段存储阿里云 suggestion（block/pass/watch）
        log.setSource(2); // 阿里云
        log.setCreatedTime(System.currentTimeMillis());
        log.setIsDelete(0);
        save(log);
    }

    @Override
    public Page<ContentCheckLogs> pageLogs(
            int page,
            int size,
            String accountId,
            Integer checkResult,
            Long startTime,
            Long endTime) {
        LambdaQueryWrapper<ContentCheckLogs> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(accountId)) {
            wrapper.eq(ContentCheckLogs::getAccountId, accountId);
        }
        if (checkResult != null) {
            wrapper.eq(ContentCheckLogs::getCheckResult, checkResult);
        }
        if (startTime != null) {
            wrapper.ge(ContentCheckLogs::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(ContentCheckLogs::getCreatedTime, endTime);
        }
        wrapper.orderByDesc(ContentCheckLogs::getCreatedTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<ContentCheckLogs> pageWatchLogs(int page, int size, Integer contentType) {
        LambdaQueryWrapper<ContentCheckLogs> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentCheckLogs::getCheckResult, RESULT_WATCH);
        if (contentType != null) {
            wrapper.eq(ContentCheckLogs::getContentType, contentType);
        }
        wrapper.orderByDesc(ContentCheckLogs::getCreatedTime);
        return page(new Page<>(page, size), wrapper);
    }
}
