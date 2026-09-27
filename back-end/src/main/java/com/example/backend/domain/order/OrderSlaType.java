package com.example.backend.domain.order;

/**
 * 订单 SLA 类型：为处于某状态的维修订单定义超时监控点、责任方与提醒接收人。
 *
 * <p>超时时长以“订单进入当前状态的时间”为基准计算。
 */
public enum OrderSlaType {
    /** 待接单超时：责任方为平台，提醒客服/管理员，超时过长自动取消。 */
    ACCEPT(RepairOrderStatus.WAITING_ACCEPT, SlaResponsibleParty.PLATFORM),
    /** 待上门超时：责任方为师傅。 */
    VISIT(RepairOrderStatus.WAITING_VISIT, SlaResponsibleParty.TECHNICIAN),
    /** 待检查超时：责任方为师傅。 */
    INSPECTION(RepairOrderStatus.WAITING_INSPECTION, SlaResponsibleParty.TECHNICIAN),
    /** 待支付（报价）超时：责任方为用户。 */
    PAYMENT(RepairOrderStatus.WAITING_PAYMENT, SlaResponsibleParty.USER),
    /** 服务中（待完工确认）超时：责任方为用户。 */
    COMPLETION(RepairOrderStatus.IN_SERVICE, SlaResponsibleParty.USER);

    private final RepairOrderStatus monitoredStatus;
    private final SlaResponsibleParty responsibleParty;

    OrderSlaType(RepairOrderStatus monitoredStatus, SlaResponsibleParty responsibleParty) {
        this.monitoredStatus = monitoredStatus;
        this.responsibleParty = responsibleParty;
    }

    /** 该 SLA 监控的订单状态。 */
    public RepairOrderStatus monitoredStatus() {
        return monitoredStatus;
    }

    /** 默认责任方。 */
    public SlaResponsibleParty responsibleParty() {
        return responsibleParty;
    }
}
