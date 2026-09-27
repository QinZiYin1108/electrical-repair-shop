package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.common.ErrorCode;
import com.example.backend.entity.AppointmentClosures;
import com.example.backend.entity.AppointmentReservations;
import com.example.backend.exception.BusinessException;
import com.example.backend.mapper.AppointmentReservationsMapper;
import com.example.backend.model.appointment.AppointmentClosureCreateRequest;
import com.example.backend.service.AppointmentCapacityService;
import com.example.backend.service.AppointmentClosuresService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 预约容量实现。用 active_slot 唯一键防止并发穿透，订单取消/改约时释放或改绑占用。 */
@Service
public class AppointmentCapacityServiceImpl
        extends ServiceImpl<AppointmentReservationsMapper, AppointmentReservations>
        implements AppointmentCapacityService {

    static final int STATUS_ACTIVE = 1;
    static final int STATUS_RELEASED = 2;
    static final String OWNER_GLOBAL = "GLOBAL";
    static final String OWNER_TECHNICIAN = "TECHNICIAN";
    static final String OWNER_STORE = "STORE";

    private static final Set<String> OWNER_TYPES =
            Set.of(OWNER_GLOBAL, OWNER_TECHNICIAN, OWNER_STORE);

    private final AppointmentClosuresService appointmentClosuresService;
    private final BusinessMetrics businessMetrics;

    public AppointmentCapacityServiceImpl(
            AppointmentClosuresService appointmentClosuresService,
            BusinessMetrics businessMetrics) {
        this.appointmentClosuresService = appointmentClosuresService;
        this.businessMetrics = businessMetrics;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserve(String technicianAccountId, Long appointmentTime, String orderId) {
        requireBookableInput(technicianAccountId, appointmentTime, orderId);
        AppointmentReservations existing = byOrderId(orderId);
        if (existing == null) {
            insertReservation(technicianAccountId, appointmentTime, orderId);
        } else {
            rebind(existing, technicianAccountId, appointmentTime, orderId);
        }
        businessMetrics.appointmentReserved();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reschedule(String orderId, String technicianAccountId, Long appointmentTime) {
        if (!StringUtils.hasText(orderId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID不能为空");
        }
        requireBookableInput(technicianAccountId, appointmentTime, null);
        AppointmentReservations existing = byOrderId(orderId);
        if (existing == null) {
            insertReservation(technicianAccountId, appointmentTime, orderId);
        } else {
            rebind(existing, technicianAccountId, appointmentTime, orderId);
        }
        businessMetrics.appointmentReserved();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int release(String orderId, String reason) {
        if (!StringUtils.hasText(orderId)) {
            return 0;
        }
        AppointmentReservations existing = byOrderId(orderId);
        if (existing == null || Integer.valueOf(STATUS_RELEASED).equals(existing.getStatus())) {
            return 0;
        }
        existing.setStatus(STATUS_RELEASED);
        existing.setActiveSlot(null);
        existing.setReleaseReason(trimToNull(reason));
        existing.setUpdatedTime(System.currentTimeMillis());
        if (!updateById(existing)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "释放预约占用失败");
        }
        businessMetrics.appointmentReleased();
        return 1;
    }

    @Override
    public void assertBookable(
            String technicianAccountId, Long appointmentTime, String excludeOrderId) {
        requireBookableInput(technicianAccountId, appointmentTime, null);
        if (isBlockedByClosure(technicianAccountId, appointmentTime)) {
            businessMetrics.appointmentCapacityRejected();
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该时段不可预约（停业/节假日/请假）");
        }
        AppointmentReservations active =
                getOne(
                        new LambdaQueryWrapper<AppointmentReservations>()
                                .eq(
                                        AppointmentReservations::getActiveSlot,
                                        activeSlotKey(technicianAccountId, appointmentTime))
                                .last("limit 1"),
                        false);
        if (active != null
                && !(StringUtils.hasText(excludeOrderId)
                        && excludeOrderId.equals(active.getOrderId()))) {
            businessMetrics.appointmentCapacityRejected();
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该预约时间已被占用，请重新选择");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppointmentClosures createClosure(
            AppointmentClosureCreateRequest request, String operatorAdminId) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数不能为空");
        }
        String ownerType =
                StringUtils.hasText(request.getOwnerType())
                        ? request.getOwnerType().trim().toUpperCase()
                        : OWNER_GLOBAL;
        if (!OWNER_TYPES.contains(ownerType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "范围仅支持 GLOBAL/TECHNICIAN/STORE");
        }
        if (!OWNER_GLOBAL.equals(ownerType) && !StringUtils.hasText(request.getOwnerId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非全平台范围需指定对象ID");
        }
        if (request.getStartTime() == null
                || request.getEndTime() == null
                || request.getStartTime() >= request.getEndTime()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "开始时间需早于结束时间");
        }
        long now = System.currentTimeMillis();
        AppointmentClosures closure = new AppointmentClosures();
        closure.setId(SnowflakeIdUtil.nextAppointmentClosureId());
        closure.setOwnerType(ownerType);
        closure.setOwnerId(trimToNull(request.getOwnerId()));
        closure.setStartTime(request.getStartTime());
        closure.setEndTime(request.getEndTime());
        closure.setReason(trimToNull(request.getReason()));
        closure.setOperatorAdminId(trimToNull(operatorAdminId));
        closure.setCreatedTime(now);
        closure.setUpdatedTime(now);
        closure.setVersion(0);
        closure.setIsDelete(0);
        appointmentClosuresService.save(closure);
        return closure;
    }

    @Override
    public int deleteClosure(String closureId) {
        if (!StringUtils.hasText(closureId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "ID不能为空");
        }
        return appointmentClosuresService.removeById(closureId) ? 1 : 0;
    }

    @Override
    public List<AppointmentClosures> listClosures(String ownerType, String ownerId, int limit) {
        LambdaQueryWrapper<AppointmentClosures> wrapper =
                new LambdaQueryWrapper<AppointmentClosures>()
                        .orderByDesc(AppointmentClosures::getCreatedTime)
                        .last("limit " + Math.min(Math.max(limit, 1), 200));
        if (StringUtils.hasText(ownerType)) {
            wrapper.eq(AppointmentClosures::getOwnerType, ownerType.trim().toUpperCase());
        }
        if (StringUtils.hasText(ownerId)) {
            wrapper.eq(AppointmentClosures::getOwnerId, ownerId);
        }
        return appointmentClosuresService.list(wrapper);
    }

    @Override
    public List<AppointmentReservations> listReservations(String technicianAccountId, int limit) {
        LambdaQueryWrapper<AppointmentReservations> wrapper =
                new LambdaQueryWrapper<AppointmentReservations>()
                        .orderByDesc(AppointmentReservations::getCreatedTime)
                        .last("limit " + Math.min(Math.max(limit, 1), 200));
        if (StringUtils.hasText(technicianAccountId)) {
            wrapper.eq(AppointmentReservations::getTechnicianAccountId, technicianAccountId);
        }
        return list(wrapper);
    }

    private void insertReservation(
            String technicianAccountId, Long appointmentTime, String orderId) {
        assertBookable(technicianAccountId, appointmentTime, orderId);
        long now = System.currentTimeMillis();
        AppointmentReservations reservation = new AppointmentReservations();
        reservation.setId(SnowflakeIdUtil.nextAppointmentReservationId());
        reservation.setTechnicianAccountId(technicianAccountId.trim());
        reservation.setAppointmentTime(appointmentTime);
        reservation.setOrderId(orderId);
        reservation.setActiveSlot(activeSlotKey(technicianAccountId, appointmentTime));
        reservation.setStatus(STATUS_ACTIVE);
        reservation.setCreatedTime(now);
        reservation.setUpdatedTime(now);
        reservation.setVersion(0);
        reservation.setIsDelete(0);
        try {
            if (!save(reservation)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "占用预约时段失败");
            }
        } catch (DuplicateKeyException ex) {
            businessMetrics.appointmentCapacityRejected();
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该预约时间已被占用，请重新选择");
        }
    }

    private void rebind(
            AppointmentReservations existing,
            String technicianAccountId,
            Long appointmentTime,
            String orderId) {
        assertBookable(technicianAccountId, appointmentTime, orderId);
        existing.setTechnicianAccountId(technicianAccountId.trim());
        existing.setAppointmentTime(appointmentTime);
        existing.setActiveSlot(activeSlotKey(technicianAccountId, appointmentTime));
        existing.setStatus(STATUS_ACTIVE);
        existing.setReleaseReason(null);
        existing.setUpdatedTime(System.currentTimeMillis());
        try {
            if (!updateById(existing)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "改绑预约时段失败");
            }
        } catch (DuplicateKeyException ex) {
            businessMetrics.appointmentCapacityRejected();
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该预约时间已被占用，请重新选择");
        }
    }

    private boolean isBlockedByClosure(String technicianAccountId, Long appointmentTime) {
        LambdaQueryWrapper<AppointmentClosures> wrapper =
                new LambdaQueryWrapper<AppointmentClosures>()
                        .le(AppointmentClosures::getStartTime, appointmentTime)
                        .gt(AppointmentClosures::getEndTime, appointmentTime)
                        .and(
                                w ->
                                        w.eq(AppointmentClosures::getOwnerType, OWNER_GLOBAL)
                                                .or(
                                                        o ->
                                                                o.eq(
                                                                                AppointmentClosures
                                                                                        ::getOwnerType,
                                                                                OWNER_TECHNICIAN)
                                                                        .eq(
                                                                                AppointmentClosures
                                                                                        ::getOwnerId,
                                                                                technicianAccountId)));
        return appointmentClosuresService.count(wrapper) > 0;
    }

    private AppointmentReservations byOrderId(String orderId) {
        return getOne(
                new LambdaQueryWrapper<AppointmentReservations>()
                        .eq(AppointmentReservations::getOrderId, orderId)
                        .last("limit 1"),
                false);
    }

    private String activeSlotKey(String technicianAccountId, Long appointmentTime) {
        return technicianAccountId.trim() + ":" + appointmentTime;
    }

    private void requireBookableInput(
            String technicianAccountId, Long appointmentTime, String orderId) {
        if (!StringUtils.hasText(technicianAccountId)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "technicianId 不能为空");
        }
        if (appointmentTime == null || appointmentTime <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "appointmentTime 不能为空");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
