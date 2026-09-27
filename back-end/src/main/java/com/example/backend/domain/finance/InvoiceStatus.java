package com.example.backend.domain.finance;

import java.util.Arrays;

/** 发票状态。 */
public enum InvoiceStatus {
    PENDING(1, "待开票"),
    ISSUED(2, "已开票"),
    RED(3, "已红冲"),
    REJECTED(4, "已驳回"),
    UNKNOWN(0, "未知");

    private final int code;
    private final String text;

    InvoiceStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static InvoiceStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
