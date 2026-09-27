package com.example.backend.domain.order;

import java.util.Arrays;

public enum RepairOrderAfterSalesStatus {
    PENDING(1),
    APPROVED(2),
    REJECTED(3),
    PROCESSING(4),
    COMPLETED(5),
    CANCELED(6),
    UNKNOWN(0);

    private final int code;

    RepairOrderAfterSalesStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public boolean isActive() {
        return this == PENDING || this == APPROVED || this == PROCESSING;
    }

    public static RepairOrderAfterSalesStatus fromCode(Integer code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
