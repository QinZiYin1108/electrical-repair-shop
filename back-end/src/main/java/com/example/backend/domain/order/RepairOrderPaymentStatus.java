package com.example.backend.domain.order;

import java.util.Arrays;

public enum RepairOrderPaymentStatus {
    PENDING(1, "待支付"),
    PAID(2, "已支付"),
    REFUNDED(3, "已退款"),
    UNKNOWN(0, "未知");

    private final int code;
    private final String text;

    RepairOrderPaymentStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static RepairOrderPaymentStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
