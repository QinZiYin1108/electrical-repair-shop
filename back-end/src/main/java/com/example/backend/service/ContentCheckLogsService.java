package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.ContentCheckLogs;

/** 内容审核日志 Service */
public interface ContentCheckLogsService extends IService<ContentCheckLogs> {

    /** 审核结果常量 */
    int RESULT_PASS = 1; // 通过

    int RESULT_BLOCK = 2; // 拦截
    int RESULT_WATCH = 4; // 存疑放行（待复审）

    /** 写入审核日志（异步）。 */
    void logCheck(
            String accountId,
            int accountType,
            int contentType,
            String content,
            int checkResult,
            String hitKeyword,
            String suggestion);

    /** 分页查询所有审核日志。 */
    Page<ContentCheckLogs> pageLogs(
            int page,
            int size,
            String accountId,
            Integer checkResult,
            Long startTime,
            Long endTime);

    /** 分页查询待复审记录（存疑放行的内容）。 */
    Page<ContentCheckLogs> pageWatchLogs(int page, int size, Integer contentType);
}
