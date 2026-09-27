package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.Reports;

/**
 * @description 针对表【reports(举报表)】的数据库操作Service
 */
public interface ReportsService extends IService<Reports> {

    /**
     * 提交举报
     *
     * @param report 举报信息
     * @return 保存后的举报记录
     */
    Reports submitReport(Reports report);

    /**
     * 管理端分页查询举报列表
     *
     * @param page 页码
     * @param size 每页条数
     * @param status 状态筛选，null-全部
     * @param targetType 对象类型筛选，null-全部
     * @param startTime 开始时间，null-不限
     * @param endTime 结束时间，null-不限
     * @return 分页结果
     */
    Page<Reports> listReports(
            int page, int size, Integer status, Integer targetType, Long startTime, Long endTime);

    /**
     * 处理举报（管理员审核）
     *
     * @param reportId 举报ID
     * @param status 新状态：3-已成立，4-已驳回
     * @param result 处理结果说明
     * @param handlerId 处理人ID
     * @return 更新后的举报记录
     */
    Reports processReport(String reportId, Integer status, String result, String handlerId);

    /**
     * 获取举报详情
     *
     * @param id 举报ID
     * @return 举报记录
     */
    Reports getReportDetail(String id);

    /**
     * 按原因分类统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计数据
     */
    java.util.List<java.util.Map<String, Object>> statsByCategory(Long startTime, Long endTime);
}
