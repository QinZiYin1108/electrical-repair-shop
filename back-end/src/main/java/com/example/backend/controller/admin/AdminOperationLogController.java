package com.example.backend.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.OperationLogs;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.admin.AdminOperationLogDetailResponse;
import com.example.backend.model.admin.AdminOperationLogListItemResponse;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.OperationLogsService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin/system/operation-logs")
public class AdminOperationLogController {

    private final OperationLogsService operationLogsService;

    public AdminOperationLogController(OperationLogsService operationLogsService) {
        this.operationLogsService = operationLogsService;
    }

    @GetMapping
    public Result<Page<AdminOperationLogListItemResponse>> listLogs(
        @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
        @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
        @RequestParam(value = "moduleName", required = false) String moduleName,
        @RequestParam(value = "operationType", required = false) String operationType,
        @RequestParam(value = "operatorType", required = false) Integer operatorType,
        @RequestParam(value = "operatorName", required = false) String operatorName,
        @RequestParam(value = "status", required = false) Integer status,
        @RequestParam(value = "startTime", required = false) Long startTime,
        @RequestParam(value = "endTime", required = false) Long endTime
    ) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        LambdaQueryWrapper<OperationLogs> wrapper = new LambdaQueryWrapper<OperationLogs>()
            .eq(OperationLogs::getIsDelete, 0);
        if (StringUtils.hasText(moduleName)) {
            wrapper.eq(OperationLogs::getModuleName, moduleName.trim());
        }
        if (StringUtils.hasText(operationType)) {
            wrapper.eq(OperationLogs::getOperationType, operationType.trim());
        }
        if (operatorType != null) {
            wrapper.eq(OperationLogs::getOperatorType, operatorType);
        }
        if (StringUtils.hasText(operatorName)) {
            wrapper.like(OperationLogs::getOperatorName, operatorName.trim());
        }
        if (status != null) {
            wrapper.eq(OperationLogs::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(OperationLogs::getCreatedTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(OperationLogs::getCreatedTime, endTime);
        }
        wrapper.orderByDesc(OperationLogs::getCreatedTime);
        Page<OperationLogs> page = operationLogsService.page(new Page<>(pageNum, pageSize), wrapper);
        List<OperationLogs> records = page.getRecords();
        List<AdminOperationLogListItemResponse> items = new ArrayList<>();
        for (OperationLogs log : records) {
            AdminOperationLogListItemResponse item = new AdminOperationLogListItemResponse();
            item.setId(log.getId());
            item.setOperatorName(log.getOperatorName());
            item.setOperatorType(log.getOperatorType());
            item.setOperationType(log.getOperationType());
            item.setOperationDesc(log.getOperationDesc());
            item.setModuleName(log.getModuleName());
            item.setRequestMethod(log.getRequestMethod());
            item.setRequestUrl(log.getRequestUrl());
            item.setStatus(log.getStatus());
            item.setCreatedTime(log.getCreatedTime());
            items.add(item);
        }
        Page<AdminOperationLogListItemResponse> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(items);
        return Result.success(resultPage);
    }

    @GetMapping("/{id}")
    public Result<AdminOperationLogDetailResponse> getDetail(@PathVariable("id") String id) {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问管理员接口");
        }
        if (!StringUtils.hasText(id)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "日志ID不能为空");
        }
        OperationLogs log = operationLogsService.getById(id);
        if (log == null || log.getIsDelete() != null && log.getIsDelete() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "操作日志不存在");
        }
        AdminOperationLogDetailResponse resp = new AdminOperationLogDetailResponse();
        resp.setId(log.getId());
        resp.setOperatorId(log.getOperatorId());
        resp.setOperatorType(log.getOperatorType());
        resp.setOperatorName(log.getOperatorName());
        resp.setOperationType(log.getOperationType());
        resp.setOperationDesc(log.getOperationDesc());
        resp.setModuleName(log.getModuleName());
        resp.setRequestMethod(log.getRequestMethod());
        resp.setRequestUrl(log.getRequestUrl());
        resp.setRequestParams(log.getRequestParams());
        resp.setResponseData(log.getResponseData());
        resp.setIpAddress(log.getIpAddress());
        resp.setUserAgent(log.getUserAgent());
        resp.setDeviceId(log.getDeviceId());
        resp.setExecutionTime(log.getExecutionTime());
        resp.setStatus(log.getStatus());
        resp.setErrorMessage(log.getErrorMessage());
        resp.setCreatedTime(log.getCreatedTime());
        return Result.success(resp);
    }
}

