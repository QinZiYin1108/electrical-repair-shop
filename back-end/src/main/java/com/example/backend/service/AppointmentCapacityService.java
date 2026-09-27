package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.AppointmentClosures;
import com.example.backend.entity.AppointmentReservations;
import com.example.backend.model.appointment.AppointmentClosureCreateRequest;
import java.util.List;

/** 预约容量：占用/释放/改绑时段占用，并校验停业/请假等不可预约区间。 */
public interface AppointmentCapacityService extends IService<AppointmentReservations> {

    /** 占用某师傅某时段的预约；已被占用或不可预约时抛异常。 */
    void reserve(String technicianAccountId, Long appointmentTime, String orderId);

    /** 改约：把订单占用改绑到新师傅/新时段；新时段不可用则抛异常。 */
    void reschedule(String orderId, String technicianAccountId, Long appointmentTime);

    /** 释放订单占用的时段，返回释放条数。 */
    int release(String orderId, String reason);

    /** 只读校验：该时段是否可预约（停业/占用），excludeOrderId 用于改约排除自身。 */
    void assertBookable(String technicianAccountId, Long appointmentTime, String excludeOrderId);

    /** 新增停业/请假时段。 */
    AppointmentClosures createClosure(
            AppointmentClosureCreateRequest request, String operatorAdminId);

    /** 删除停业/请假时段，返回删除条数。 */
    int deleteClosure(String closureId);

    /** 查询停业/请假时段。 */
    List<AppointmentClosures> listClosures(String ownerType, String ownerId, int limit);

    /** 查询某师傅的时段占用。 */
    List<AppointmentReservations> listReservations(String technicianAccountId, int limit);
}
