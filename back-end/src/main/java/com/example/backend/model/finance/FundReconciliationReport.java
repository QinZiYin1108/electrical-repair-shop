package com.example.backend.model.finance;

import java.util.List;

public record FundReconciliationReport(long checkedAt, List<String> issues) {

    public boolean isHealthy() {
        return issues == null || issues.isEmpty();
    }
}
