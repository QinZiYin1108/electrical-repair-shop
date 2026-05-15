package com.example.backend.model.worker;

import java.util.ArrayList;
import java.util.List;

public class WorkerHomeOrdersResponse {

    private Integer waitingCount;
    private Integer inProgressCount;
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
