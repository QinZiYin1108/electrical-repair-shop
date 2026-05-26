package com.example.backend.model.admin;

public class AdminWorkerOrderStatsResponse {

    private Long totalCount;
    private Long waitingCount;
    private Long ongoingCount;
    private Long waitingPayCount;
    private Long completedCount;
    private Long canceledCount;
    private Long refundedCount;
    private Long latestOrderTime;

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(Long waitingCount) {
        this.waitingCount = waitingCount;
    }

    public Long getOngoingCount() {
        return ongoingCount;
    }

    public void setOngoingCount(Long ongoingCount) {
        this.ongoingCount = ongoingCount;
    }

    public Long getWaitingPayCount() {
        return waitingPayCount;
    }

    public void setWaitingPayCount(Long waitingPayCount) {
        this.waitingPayCount = waitingPayCount;
    }

    public Long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(Long completedCount) {
        this.completedCount = completedCount;
    }

    public Long getCanceledCount() {
        return canceledCount;
    }

    public void setCanceledCount(Long canceledCount) {
        this.canceledCount = canceledCount;
    }

    public Long getRefundedCount() {
        return refundedCount;
    }

    public void setRefundedCount(Long refundedCount) {
        this.refundedCount = refundedCount;
    }

    public Long getLatestOrderTime() {
        return latestOrderTime;
    }

    public void setLatestOrderTime(Long latestOrderTime) {
        this.latestOrderTime = latestOrderTime;
    }
}
