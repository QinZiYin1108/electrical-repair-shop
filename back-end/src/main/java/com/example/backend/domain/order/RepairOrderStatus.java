package com.example.backend.domain.order;

import java.util.Arrays;

public enum RepairOrderStatus {
    WAITING_ACCEPT(1, "待接单"),
    WAITING_VISIT(2, "待上门"),
    WAITING_INSPECTION(3, "待检查"),
    WAITING_PAYMENT(4, "待支付"),
    IN_SERVICE(5, "服务中"),
    COMPLETED(6, "已完成"),
    CANCELED(7, "已取消"),
    REFUNDED(8, "已退款"),
    UNKNOWN(0, "未知状态");

    private final int code;
    private final String text;

    RepairOrderStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public boolean isClosed() {
        return this == COMPLETED || this == CANCELED || this == REFUNDED;
    }

    public static RepairOrderStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
