package com.example.backend.domain.support;

import java.util.Arrays;

/** 客服工单状态。 */
public enum SupportTicketStatus {
    PENDING(1, "待处理"),
    PROCESSING(2, "处理中"),
    PENDING_APPROVAL(3, "待审批"),
    RESOLVED(4, "已解决"),
    CLOSED(5, "已关闭"),
    UNKNOWN(0, "未知");

    private final int code;
    private final String text;

    SupportTicketStatus(int code, String text) {
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
        return this == RESOLVED || this == CLOSED;
    }

    public static SupportTicketStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
