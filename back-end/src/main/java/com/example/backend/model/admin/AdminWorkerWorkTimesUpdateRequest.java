package com.example.backend.model.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AdminWorkerWorkTimesUpdateRequest {

    @NotEmpty(message = "工作时间不能为空")
    @Valid
    private List<WorkTimeItem> workTimes;

    public List<WorkTimeItem> getWorkTimes() {
        return workTimes;
    }

    public void setWorkTimes(List<WorkTimeItem> workTimes) {
        this.workTimes = workTimes;
    }

    public static class WorkTimeItem {

        private String id;

        @NotNull(message = "dayOfWeek 不能为空")
        private Integer dayOfWeek;

        @NotBlank(message = "startTime 不能为空")
        private String startTime;

        @NotBlank(message = "endTime 不能为空")
        private String endTime;

        private Integer isAvailable;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(Integer dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public Integer getIsAvailable() {
            return isAvailable;
        }

        public void setIsAvailable(Integer isAvailable) {
            this.isAvailable = isAvailable;
        }
    }
}
