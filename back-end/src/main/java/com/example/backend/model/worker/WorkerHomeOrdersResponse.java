package com.example.backend.model.worker;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "师傅首页Orders响应")
public class WorkerHomeOrdersResponse {

    @Schema(description = "waiting数量")
    private Integer waitingCount;

    @Schema(description = "in进度数量")
    private Integer inProgressCount;

    @Schema(description = "总数是否启用数量")
    private Integer totalActiveCount;

    private List<WorkerHomeOrderItem> waitingOrders = new ArrayList<>();
    private List<WorkerHomeOrderItem> inProgressOrders = new ArrayList<>();

    public Integer getWaitingCount() {
        return waitingCount;
    }

    public void setWaitingCount(Integer waitingCount) {
        this.waitingCount = waitingCount;
    }

    public Integer getInProgressCount() {
        return inProgressCount;
    }

    public void setInProgressCount(Integer inProgressCount) {
        this.inProgressCount = inProgressCount;
    }

    public Integer getTotalActiveCount() {
        return totalActiveCount;
    }

    public void setTotalActiveCount(Integer totalActiveCount) {
        this.totalActiveCount = totalActiveCount;
    }

    public List<WorkerHomeOrderItem> getWaitingOrders() {
        return waitingOrders;
    }

    public void setWaitingOrders(List<WorkerHomeOrderItem> waitingOrders) {
        this.waitingOrders = waitingOrders;
    }

    public List<WorkerHomeOrderItem> getInProgressOrders() {
        return inProgressOrders;
    }

    public void setInProgressOrders(List<WorkerHomeOrderItem> inProgressOrders) {
        this.inProgressOrders = inProgressOrders;
    }
}
