package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.Reports;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.ReportsMapper;
import com.example.backend.service.ContentCheckLogsService;
import com.example.backend.service.ReportsService;
import com.example.backend.service.SystemConfigsService;
import com.example.backend.service.contentcheck.AliyunGreenClient;
import com.example.backend.service.contentcheck.CheckResult;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @description 针对表【reports(举报表)】的数据库操作Service实现
 */
@Service
public class ReportsServiceImpl extends ServiceImpl<ReportsMapper, Reports>
        implements ReportsService {

    /** 每人每天最大举报次数（默认值，优先从 system_configs 读取） */
    private static final int DAILY_REPORT_LIMIT = 10;

    private final AliyunGreenClient aliyunGreenClient;
    private final ContentCheckLogsService checkLogsService;
    private final SystemConfigsService systemConfigsService;

    public ReportsServiceImpl(
            AliyunGreenClient aliyunGreenClient,
            ContentCheckLogsService checkLogsService,
            SystemConfigsService systemConfigsService) {
        this.aliyunGreenClient = aliyunGreenClient;
        this.checkLogsService = checkLogsService;
        this.systemConfigsService = systemConfigsService;
    }

    @Override
    public Reports submitReport(Reports report) {
        // 1. 校验必填字段
        if (report.getReporterId() == null
                || report.getReporterType() == null
                || report.getTargetType() == null
                || report.getTargetId() == null
                || report.getReasonCategory() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "举报信息不完整");
        }

        // 2. 检查是否已有未处理的重复举报
        long duplicateCount =
                count(
                        new LambdaQueryWrapper<Reports>()
                                .eq(Reports::getReporterId, report.getReporterId())
                                .eq(Reports::getTargetType, report.getTargetType())
                                .eq(Reports::getTargetId, report.getTargetId())
                                .in(Reports::getStatus, 1, 2) // 待处理 / 处理中
                        );
        if (duplicateCount > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_KEY, "您已对该对象提交过举报，请等待处理结果");
        }

        // 3. 检查每日举报次数限制（优先从 system_configs 读取 report.daily_limit）
        int dailyLimit =
                systemConfigsService.getIntegerConfig("report.daily_max_count", DAILY_REPORT_LIMIT);
        long todayStart = getTodayStartMs();
        long todayCount =
                count(
                        new LambdaQueryWrapper<Reports>()
                                .eq(Reports::getReporterId, report.getReporterId())
                                .ge(Reports::getCreatedTime, todayStart));
        if (todayCount >= dailyLimit) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR, "今日举报次数已达上限（" + dailyLimit + "次），请明天再试");
        }

        // 4. 举报说明文字检测
        String description = report.getDescription();
        if (StringUtils.hasText(description)) {
            CheckResult cr = aliyunGreenClient.checkText(description);
            int logResult =
                    cr.isBlocked()
                            ? ContentCheckLogsService.RESULT_BLOCK
                            : cr.isWatch()
                                    ? ContentCheckLogsService.RESULT_WATCH
                                    : ContentCheckLogsService.RESULT_PASS;
            int accountType = report.getReporterType() != null ? report.getReporterType() : 1;
            checkLogsService.logCheck(
                    report.getReporterId(),
                    accountType,
                    1,
                    description,
                    logResult,
                    cr.getLabel(),
                    cr.getSuggestion());
            if (cr.isBlocked()) {
                throw new BusinessException(
                        ErrorCode.BUSINESS_ERROR, "举报说明包含违规内容：" + cr.getLabelDesc() + "，请修改后重新提交");
            }
        }

        // 5. 保存
        long now = System.currentTimeMillis();
        report.setId(SnowflakeIdUtil.nextReportId());
        report.setStatus(1); // 待处理
        report.setCreatedTime(now);
        report.setUpdatedTime(now);
        report.setVersion(1);
        report.setIsDelete(0);
        save(report);
        return report;
    }

    @Override
    public Page<Reports> listReports(
            int page, int size, Integer status, Integer targetType, Long startTime, Long endTime) {
        LambdaQueryWrapper<Reports> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Reports::getStatus, status);
        }
        if (targetType != null) {
            wrapper.eq(Reports::getTargetType, targetType);
        }
        if (startTime != null) {
            wrapper.ge(Reports::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Reports::getCreatedTime, endTime);
        }
        wrapper.orderByDesc(Reports::getCreatedTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public Reports processReport(String reportId, Integer status, String result, String handlerId) {
        Reports report = getById(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "举报不存在");
        }
        // 状态校验：只能从待处理/处理中转出
        if (report.getStatus() != 1 && report.getStatus() != 2) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该举报已处理，无法重复操作");
        }
        if (status != 3 && status != 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "处理结果只能为'已成立'或'已驳回'");
        }

        long now = System.currentTimeMillis();
        report.setStatus(status);
        report.setResult(result);
        report.setHandlerId(handlerId);
        report.setHandleTime(now);
        report.setUpdatedTime(now);
        updateById(report);
        return report;
    }

    @Override
    public Reports getReportDetail(String id) {
        Reports report = getById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "举报不存在");
        }
        return report;
    }

    @Override
    public List<Map<String, Object>> statsByCategory(Long startTime, Long endTime) {
        LambdaQueryWrapper<Reports> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(Reports::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Reports::getCreatedTime, endTime);
        }
        wrapper.select(Reports::getReasonCategory, Reports::getStatus);
        List<Reports> records = list(wrapper);

        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (Reports r : records) {
            String cat = r.getReasonCategory() != null ? r.getReasonCategory() : "未分类";
            grouped.computeIfAbsent(
                    cat,
                    k -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("category", k);
                        m.put("total", 0L);
                        m.put("approved", 0L);
                        m.put("rejected", 0L);
                        m.put("pending", 0L);
                        return m;
                    });
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) grouped.get(cat);
            entry.put("total", ((Long) entry.get("total")) + 1);
            if (r.getStatus() != null && r.getStatus() == 3) {
                entry.put("approved", ((Long) entry.get("approved")) + 1);
            } else if (r.getStatus() != null && r.getStatus() == 4) {
                entry.put("rejected", ((Long) entry.get("rejected")) + 1);
            } else {
                entry.put("pending", ((Long) entry.get("pending")) + 1);
            }
        }
        return new ArrayList<>(grouped.values());
    }

    /** 获取今日零点毫秒时间戳 */
    private long getTodayStartMs() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
