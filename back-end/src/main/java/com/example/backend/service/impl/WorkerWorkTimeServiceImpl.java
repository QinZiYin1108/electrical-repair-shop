package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.TechnicianWorkTimes;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.worker.WorkerWorkTimeItem;
import com.example.backend.model.worker.WorkerWorkTimesUpdateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.AccountRole;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.TechnicianWorkTimesService;
import com.example.backend.service.WorkerWorkTimeService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkerWorkTimeServiceImpl implements WorkerWorkTimeService {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_END_TIME = LocalTime.of(18, 0);
    private static final DateTimeFormatter SIMPLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TechnicianWorkTimesService technicianWorkTimesService;

    public WorkerWorkTimeServiceImpl(TechnicianWorkTimesService technicianWorkTimesService) {
        this.technicianWorkTimesService = technicianWorkTimesService;
    }

    @Override
    public List<WorkerWorkTimeItem> getCurrentWorkerWorkTimes() {
        String accountId = requireWorker().getAccountId();
        List<TechnicianWorkTimes> existingList = technicianWorkTimesService.list(
            new LambdaQueryWrapper<TechnicianWorkTimes>()
                .eq(TechnicianWorkTimes::getTechnicianAccountId, accountId)
                .eq(TechnicianWorkTimes::getIsDelete, 0)
                .orderByAsc(TechnicianWorkTimes::getDayOfWeek)
                .orderByDesc(TechnicianWorkTimes::getUpdatedTime)
                .orderByDesc(TechnicianWorkTimes::getCreatedTime)
        );

        Map<Integer, TechnicianWorkTimes> dayToRecordMap = new LinkedHashMap<>();
        for (TechnicianWorkTimes item : existingList) {
            if (item == null || item.getDayOfWeek() == null) {
                continue;
            }
            Integer dayOfWeek = item.getDayOfWeek();
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                continue;
            }
            if (!dayToRecordMap.containsKey(dayOfWeek)) {
                dayToRecordMap.put(dayOfWeek, item);
            }
        }

        List<WorkerWorkTimeItem> responseList = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            responseList.add(toResponseItem(dayToRecordMap.get(day), day));
        }
        return responseList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCurrentWorkerWorkTimes(WorkerWorkTimesUpdateRequest request) {
        String accountId = requireWorker().getAccountId();
        if (request == null || request.getWorkTimes() == null || request.getWorkTimes().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "工作时间不能为空");
        }

        Map<Integer, WorkerWorkTimeItem> dayToItemMap = new LinkedHashMap<>();
        for (WorkerWorkTimeItem item : request.getWorkTimes()) {
            if (item == null || item.getDayOfWeek() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "工作时间参数不完整");
            }
            Integer dayOfWeek = item.getDayOfWeek();
            if (dayOfWeek < 1 || dayOfWeek > 7) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "dayOfWeek 仅支持 1-7");
            }
            if (dayToItemMap.containsKey(dayOfWeek)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "同一天只能提交一次");
            }
            dayToItemMap.put(dayOfWeek, item);
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, WorkerWorkTimeItem> entry : dayToItemMap.entrySet()) {
            Integer dayOfWeek = entry.getKey();
            WorkerWorkTimeItem item = entry.getValue();
            boolean available = item.getIsAvailable() == null || item.getIsAvailable() == 1;
            if (item.getIsAvailable() != null && item.getIsAvailable() != 0 && item.getIsAvailable() != 1) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "isAvailable 仅支持 0 或 1");
            }

            LocalTime startTime = parseTimeOrDefault(item.getStartTime(), DEFAULT_START_TIME, available, "startTime");
            LocalTime endTime = parseTimeOrDefault(item.getEndTime(), DEFAULT_END_TIME, available, "endTime");
            if (!startTime.isBefore(endTime)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间必须早于结束时间");
            }

            TechnicianWorkTimes workTime = findAnyByDay(accountId, dayOfWeek);
            boolean isNew = workTime == null;
            if (isNew) {
                workTime = new TechnicianWorkTimes();
                workTime.setId(SnowflakeIdUtil.nextTechnicianWorkTimeId());
                workTime.setTechnicianAccountId(accountId);
                workTime.setDayOfWeek(dayOfWeek);
                workTime.setCreatedTime(now);
                workTime.setIsDelete(0);
            }

            workTime.setStartTime(Time.valueOf(startTime));
            workTime.setEndTime(Time.valueOf(endTime));
            workTime.setIsAvailable(available ? 1 : 0);
            workTime.setUpdatedTime(now);
            workTime.setIsDelete(0);

            if (isNew) {
                technicianWorkTimesService.save(workTime);
            } else {
                technicianWorkTimesService.updateById(workTime);
            }
        }
    }

    private TechnicianWorkTimes findAnyByDay(String accountId, Integer dayOfWeek) {
        return technicianWorkTimesService.getOne(
            new LambdaQueryWrapper<TechnicianWorkTimes>()
                .eq(TechnicianWorkTimes::getTechnicianAccountId, accountId)
                .eq(TechnicianWorkTimes::getDayOfWeek, dayOfWeek)
                .orderByDesc(TechnicianWorkTimes::getUpdatedTime)
                .orderByDesc(TechnicianWorkTimes::getCreatedTime)
                .last("limit 1"),
            false
        );
    }

    private WorkerWorkTimeItem toResponseItem(TechnicianWorkTimes entity, Integer defaultDay) {
        WorkerWorkTimeItem item = new WorkerWorkTimeItem();
        if (entity == null) {
            item.setDayOfWeek(defaultDay);
            item.setStartTime(formatTime(DEFAULT_START_TIME));
            item.setEndTime(formatTime(DEFAULT_END_TIME));
            item.setIsAvailable(1);
            return item;
        }
        item.setId(entity.getId());
        item.setDayOfWeek(entity.getDayOfWeek() == null ? defaultDay : entity.getDayOfWeek());
        item.setStartTime(formatTime(toLocalTime(entity.getStartTime(), DEFAULT_START_TIME)));
        item.setEndTime(formatTime(toLocalTime(entity.getEndTime(), DEFAULT_END_TIME)));
        item.setIsAvailable(entity.getIsAvailable() != null && entity.getIsAvailable() == 0 ? 0 : 1);
        return item;
    }

    private LocalTime parseTimeOrDefault(String value, LocalTime defaultValue, boolean required, String fieldName) {
        if (!StringUtils.hasText(value)) {
            if (required) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 不能为空");
            }
            return defaultValue;
        }
        String normalized = value.trim();
        try {
            if (normalized.length() == 5) {
                return LocalTime.parse(normalized, SIMPLE_TIME_FORMATTER);
            }
            return LocalTime.parse(normalized, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " 格式错误，应为 HH:mm 或 HH:mm:ss");
        }
    }

    private LocalTime toLocalTime(java.util.Date value, LocalTime defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Time) {
            return ((Time) value).toLocalTime();
        }
        return Instant.ofEpochMilli(value.getTime())
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .withNano(0);
    }

    private String formatTime(LocalTime value) {
        return TIME_FORMATTER.format(value.withNano(0));
    }

    private LoginUserInfo requireWorker() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null || !StringUtils.hasText(user.getAccountId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        if (user.getRole() != AccountRole.WORKER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问师傅接口");
        }
        return user;
    }
}
