package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backend.domain.order.OrderSlaType;
import com.example.backend.domain.order.RepairOrderPaymentStatus;
import com.example.backend.domain.order.RepairOrderStateMachine;
import com.example.backend.domain.order.RepairOrderStatus;
import com.example.backend.entity.OrderProgress;
import com.example.backend.entity.OrderSlaEvents;
import com.example.backend.entity.RepairOrders;
import com.example.backend.mapper.OrderSlaEventsMapper;
import com.example.backend.service.AppointmentCapacityService;
import com.example.backend.service.BusinessMetrics;
import com.example.backend.service.NotificationOutboxService;
import com.example.backend.service.OrderProgressService;
import com.example.backend.service.OrderSlaService;
import com.example.backend.service.RepairOrderCommandService;
import com.example.backend.service.RepairOrdersService;
import com.example.backend.utils.id.SnowflakeIdUtil;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 订单 SLA 超时引擎实现。以“订单进入当前状态的时间”（{@code updatedTime}）为基准判断超时：
 * 首次超时创建事件并提醒责任方；持续超时按倍数升级并提醒管理员；未接单超时达上限时经订单状态机自动取消。
 */
@Service
public class OrderSlaServiceImpl extends ServiceImpl<OrderSlaEventsMapper, OrderSlaEvents>
        implements OrderSlaService {

    static final int ORDER_TYPE_REPAIR = 1;
    static final int EVENT_STATUS_ACTIVE = 1;
    static final int EVENT_STATUS_RECOVERED = 2;
    static final int EVENT_STATUS_AUTO_HANDLED = 3;
    static final int ESCALATION_REMIND = 1;
    static final int ESCALATION_ESCALATED = 2;
    static final String ACTION_REMIND = "REMIND";
    static final String ACTION_ESCALATE = "ESCALATE";
    static final String ACTION_AUTO_CANCEL = "AUTO_CANCEL";
    static final String PARTY_PLATFORM = "PLATFORM";

    private static final int RECEIVER_TYPE_USER = 1;
    private static final int RECEIVER_TYPE_TECHNICIAN = 2;
    private static final int RECEIVER_TYPE_ADMIN = 3;
    private static final int OPERATOR_TYPE_SYSTEM = 4;
    private static final int ESCALATE_MULTIPLIER = 2;

    private static final Map<OrderSlaType, String> TYPE_LABELS = new EnumMap<>(OrderSlaType.class);

    static {
        TYPE_LABELS.put(OrderSlaType.ACCEPT, "待接单");
        TYPE_LABELS.put(OrderSlaType.VISIT, "待上门");
        TYPE_LABELS.put(OrderSlaType.INSPECTION, "待检查");
        TYPE_LABELS.put(OrderSlaType.PAYMENT, "待支付");
        TYPE_LABELS.put(OrderSlaType.COMPLETION, "完工确认");
    }

    private final RepairOrdersService repairOrdersService;
    private final RepairOrderStateMachine stateMachine;
    private final RepairOrderCommandService repairOrderCommandService;
    private final OrderProgressService orderProgressService;
    private final NotificationOutboxService notificationOutboxService;
    private final AppointmentCapacityService appointmentCapacityService;
    private final BusinessMetrics businessMetrics;

    @Value("${order.sla.accept.remind-minutes:30}")
    private int acceptRemindMinutes = 30;

    @Value("${order.sla.accept.cancel-minutes:120}")
    private int acceptCancelMinutes = 120;

    @Value("${order.sla.visit.remind-minutes:240}")
    private int visitRemindMinutes = 240;

    @Value("${order.sla.inspection.remind-minutes:120}")
    private int inspectionRemindMinutes = 120;

    @Value("${order.sla.payment.remind-minutes:1440}")
    private int paymentRemindMinutes = 1440;

    @Value("${order.sla.completion.remind-minutes:1440}")
    private int completionRemindMinutes = 1440;

    @Value("${order.sla.scan-batch-size:200}")
    private int scanBatchSize = 200;

    @Value("${order.sla.admin-account-id:}")
    private String adminAccountId;

    public OrderSlaServiceImpl(
            RepairOrdersService repairOrdersService,
            RepairOrderStateMachine stateMachine,
            RepairOrderCommandService repairOrderCommandService,
            OrderProgressService orderProgressService,
            NotificationOutboxService notificationOutboxService,
            AppointmentCapacityService appointmentCapacityService,
            BusinessMetrics businessMetrics) {
        this.repairOrdersService = repairOrdersService;
        this.stateMachine = stateMachine;
        this.repairOrderCommandService = repairOrderCommandService;
        this.orderProgressService = orderProgressService;
        this.notificationOutboxService = notificationOutboxService;
        this.appointmentCapacityService = appointmentCapacityService;
        this.businessMetrics = businessMetrics;
    }

    @Override
    public int scanAndHandle(long now) {
        int handled = resolveStaleEvents(now);
        for (OrderSlaType type : OrderSlaType.values()) {
            handled += scanType(type, now);
        }
        return handled;
    }

    @Override
    public List<OrderSlaEvents> listForAdmin(String orderId, Integer status, int limit) {
        LambdaQueryWrapper<OrderSlaEvents> wrapper =
                new LambdaQueryWrapper<OrderSlaEvents>()
                        .orderByDesc(OrderSlaEvents::getCreatedTime)
                        .last("limit " + Math.min(Math.max(limit, 1), 200));
        if (StringUtils.hasText(orderId)) {
            wrapper.eq(OrderSlaEvents::getOrderId, orderId);
        }
        if (status != null) {
            wrapper.eq(OrderSlaEvents::getStatus, status);
        }
        return list(wrapper);
    }

    /** 处理已恢复的事件：订单已离开监控状态（或不存在）时置为已恢复。 */
    int resolveStaleEvents(long now) {
        List<OrderSlaEvents> active =
                list(
                        new LambdaQueryWrapper<OrderSlaEvents>()
                                .eq(OrderSlaEvents::getStatus, EVENT_STATUS_ACTIVE));
        int handled = 0;
        for (OrderSlaEvents event : active) {
            OrderSlaType type = parseType(event.getSlaType());
            if (type == null) {
                continue;
            }
            RepairOrders order = repairOrdersService.getById(event.getOrderId());
            boolean stillOverdue =
                    order != null
                            && order.getStatus() != null
                            && order.getStatus() == type.monitoredStatus().getCode();
            if (!stillOverdue) {
                event.setStatus(EVENT_STATUS_RECOVERED);
                event.setResolvedTime(now);
                event.setUpdatedTime(now);
                updateById(event);
                businessMetrics.orderSlaRecovered(event.getSlaType());
                handled++;
            }
        }
        return handled;
    }

    private int scanType(OrderSlaType type, long now) {
        int remindMinutes = remindMinutesFor(type);
        if (remindMinutes <= 0) {
            return 0;
        }
        long threshold = now - remindMinutes * 60_000L;
        List<RepairOrders> orders =
                repairOrdersService.list(
                        new LambdaQueryWrapper<RepairOrders>()
                                .eq(RepairOrders::getStatus, type.monitoredStatus().getCode())
                                .le(RepairOrders::getUpdatedTime, threshold)
                                .orderByAsc(RepairOrders::getUpdatedTime)
                                .last("limit " + Math.max(scanBatchSize, 1)));
        int handled = 0;
        for (RepairOrders order : orders) {
            if (processOverdueOrder(order, type, now)) {
                handled++;
            }
        }
        return handled;
    }

    /** 处理单个超时订单：创建/更新超时事件、提醒、升级或自动取消。 */
    boolean processOverdueOrder(RepairOrders order, OrderSlaType type, long now) {
        int remindMinutes = remindMinutesFor(type);
        long base =
                order.getUpdatedTime() != null ? order.getUpdatedTime() : order.getCreatedTime();
        long overdueMillis = now - base;
        if (overdueMillis < remindMinutes * 60_000L) {
            return false;
        }
        int overdueMinutes = (int) (overdueMillis / 60_000L);

        OrderSlaEvents event = findActive(order.getId(), type);
        if (event == null) {
            event = newActiveEvent(order, type, now, overdueMinutes);
            save(event);
            boolean notified = notify(event, order, type, now, ACTION_REMIND, false);
            event.setEscalationLevel(ESCALATION_REMIND);
            event.setActionTaken(ACTION_REMIND);
            if (notified) {
                event.setNotifyCount(1);
                event.setLastNotifiedTime(now);
            }
            event.setUpdatedTime(now);
            updateById(event);
            businessMetrics.orderSlaExceeded(type.name());
            return true;
        }

        event.setOverdueMinutes(overdueMinutes);
        event.setUpdatedTime(now);
        if (type == OrderSlaType.ACCEPT
                && overdueMinutes >= acceptCancelMinutes
                && isAutoCancelEligible(order)
                && autoCancel(order, now)) {
            event.setStatus(EVENT_STATUS_AUTO_HANDLED);
            event.setActionTaken(ACTION_AUTO_CANCEL);
            event.setResolvedTime(now);
            businessMetrics.orderSlaAutoCanceled(type.name());
        } else if (overdueMinutes >= remindMinutes * ESCALATE_MULTIPLIER
                && (event.getEscalationLevel() == null
                        || event.getEscalationLevel() < ESCALATION_ESCALATED)) {
            event.setEscalationLevel(ESCALATION_ESCALATED);
            event.setActionTaken(ACTION_ESCALATE);
            boolean notified = notify(event, order, type, now, ACTION_ESCALATE, true);
            if (notified) {
                event.setNotifyCount(notifyCountOf(event) + 1);
                event.setLastNotifiedTime(now);
            }
            businessMetrics.orderSlaEscalated(type.name());
        }
        updateById(event);
        return true;
    }

    private OrderSlaEvents newActiveEvent(
            RepairOrders order, OrderSlaType type, long now, int overdueMinutes) {
        OrderSlaEvents event = new OrderSlaEvents();
        event.setId(SnowflakeIdUtil.nextOrderSlaEventId());
        event.setOrderId(order.getId());
        event.setOrderType(ORDER_TYPE_REPAIR);
        event.setSlaType(type.name());
        event.setOrderStatus(order.getStatus());
        event.setResponsibleParty(type.responsibleParty().name());
        event.setResponsibleId(resolveResponsibleId(order, type));
        event.setStatus(EVENT_STATUS_ACTIVE);
        event.setEscalationLevel(0);
        event.setNotifyCount(0);
        event.setOverdueMinutes(overdueMinutes);
        event.setFirstExceededTime(now);
        event.setCreatedTime(now);
        event.setUpdatedTime(now);
        event.setVersion(0);
        event.setIsDelete(0);
        return event;
    }

    private boolean isAutoCancelEligible(RepairOrders order) {
        return order.getStatus() != null
                && order.getStatus() == RepairOrderStatus.WAITING_ACCEPT.getCode()
                && RepairOrderPaymentStatus.fromCode(order.getPaymentStatus())
                        != RepairOrderPaymentStatus.PAID;
    }

    private boolean autoCancel(RepairOrders order, long now) {
        int from = order.getStatus();
        int target = RepairOrderStatus.CANCELED.getCode();
        stateMachine.requireTransition(from, target);
        order.setCancelReason("系统自动取消：超时未接单");
        order.setCancelTime(now);
        repairOrderCommandService.saveTransition(order, from, target, now, "自动取消订单失败");
        saveProgress(order, "超时未接单，系统自动取消", now);
        appointmentCapacityService.release(order.getId(), "超时未接单自动取消");
        return true;
    }

    private boolean notify(
            OrderSlaEvents event,
            RepairOrders order,
            OrderSlaType type,
            long now,
            String action,
            boolean escalate) {
        String recipientId;
        int receiverType;
        if (escalate) {
            recipientId = trimToNull(adminAccountId);
            receiverType = RECEIVER_TYPE_ADMIN;
        } else {
            switch (type.responsibleParty()) {
                case TECHNICIAN -> {
                    recipientId = order.getTechnicianAccountId();
                    receiverType = RECEIVER_TYPE_TECHNICIAN;
                }
                case USER -> {
                    recipientId = order.getAccountId();
                    receiverType = RECEIVER_TYPE_USER;
                }
                default -> {
                    recipientId = trimToNull(adminAccountId);
                    receiverType = RECEIVER_TYPE_ADMIN;
                }
            }
        }
        if (!StringUtils.hasText(recipientId)) {
            return false;
        }
        String label = TYPE_LABELS.getOrDefault(type, type.name());
        String title =
                ACTION_ESCALATE.equals(action)
                        ? "订单超时升级提醒（" + label + "）"
                        : "订单超时提醒（" + label + "）";
        String content =
                "订单 "
                        + order.getOrderNo()
                        + " 在“"
                        + label
                        + "”阶段停留已超过 "
                        + event.getOverdueMinutes()
                        + " 分钟，请及时处理。";
        String dedupKey =
                "SLA:"
                        + type.name()
                        + ":"
                        + order.getId()
                        + ":"
                        + action
                        + ":"
                        + (notifyCountOf(event) + 1);
        notificationOutboxService.enqueueInApp(
                "ORDER_SLA_" + action,
                recipientId,
                receiverType,
                title,
                content,
                "REPAIR_ORDER",
                order.getId(),
                dedupKey);
        return true;
    }

    private String resolveResponsibleId(RepairOrders order, OrderSlaType type) {
        return switch (type.responsibleParty()) {
            case TECHNICIAN -> order.getTechnicianAccountId();
            case USER -> order.getAccountId();
            default -> trimToNull(adminAccountId);
        };
    }

    private void saveProgress(RepairOrders order, String description, long now) {
        OrderProgress progress = new OrderProgress();
        progress.setId(SnowflakeIdUtil.nextOrderProgressId());
        progress.setOrderId(order.getId());
        progress.setStatus(order.getStatus());
        progress.setStatusName(stateMachine.statusText(order.getStatus()));
        progress.setDescription(description);
        progress.setOperatorId("SYSTEM");
        progress.setOperatorType(OPERATOR_TYPE_SYSTEM);
        progress.setOperatorName("系统");
        progress.setCreatedTime(now);
        progress.setVersion(0);
        progress.setIsDelete(0);
        orderProgressService.save(progress);
    }

    private OrderSlaEvents findActive(String orderId, OrderSlaType type) {
        return getOne(
                new LambdaQueryWrapper<OrderSlaEvents>()
                        .eq(OrderSlaEvents::getOrderId, orderId)
                        .eq(OrderSlaEvents::getSlaType, type.name())
                        .eq(OrderSlaEvents::getStatus, EVENT_STATUS_ACTIVE)
                        .last("limit 1"),
                false);
    }

    int remindMinutesFor(OrderSlaType type) {
        return switch (type) {
            case ACCEPT -> acceptRemindMinutes;
            case VISIT -> visitRemindMinutes;
            case INSPECTION -> inspectionRemindMinutes;
            case PAYMENT -> paymentRemindMinutes;
            case COMPLETION -> completionRemindMinutes;
        };
    }

    private static OrderSlaType parseType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OrderSlaType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int notifyCountOf(OrderSlaEvents event) {
        return event.getNotifyCount() == null ? 0 : event.getNotifyCount();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
