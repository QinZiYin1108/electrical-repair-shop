package com.example.backend.model.admin;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class AdminStoreBusinessHoursRequest {

    @Valid
    private List<AdminStoreBusinessHourItem> hours;
}
