package com.example.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.backend.entity.OrderSlaEvents;
import java.util.List;

/** 订单 SLA / 超时引擎：扫描超时订单，记录超时事件、提醒责任方、升级并（未接单时）自动取消。 */
public interface OrderSlaService extends IService<OrderSlaEvents> {

    /** 执行一次扫描与处理，返回本次创建/更新/处理的事件条数。 */
    int scanAndHandle(long now);

    /** 管理端查询超时事件（orderId/status 为 null 时不过滤）。 */
    List<OrderSlaEvents> listForAdmin(String orderId, Integer status, int limit);
}
