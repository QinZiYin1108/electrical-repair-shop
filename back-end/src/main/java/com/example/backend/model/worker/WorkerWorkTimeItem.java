package com.example.backend.model.worker;

import lombok.Data;

@Data
public class WorkerWorkTimeItem {

    private String id;

    /**
     * 1-7 (周一到周日)
     */
    private Integer dayOfWeek;

    /**
     * HH:mm:ss
     */
    private String startTime;

    /**
     * HH:mm:ss
     */
    private String endTime;

    /**
     * 0-不可接单，1-可接单
     */
    private Integer isAvailable;
}
