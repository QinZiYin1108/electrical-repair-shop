package com.example.backend.domain.support;

import java.util.Arrays;

/** 客服工单优先级。 */
public enum SupportTicketPriority {
    HIGH(1, "高"),
    MEDIUM(2, "中"),
    LOW(3, "低");

    private final int code;
    private final String text;

    SupportTicketPriority(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(priority -> priority.code == code);
    }
}
