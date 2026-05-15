package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserOrderDoorQrResponse;
import com.example.backend.service.OrderDoorQrService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pass/door-qr")
public class DoorQrPublicController {

    private final OrderDoorQrService orderDoorQrService;

    public DoorQrPublicController(OrderDoorQrService orderDoorQrService) {
        this.orderDoorQrService = orderDoorQrService;
    }

    @GetMapping("/scan")
    public Result<UserOrderDoorQrResponse> scan(@RequestParam("token") String token) {
        return Result.success(orderDoorQrService.getDoorQrByToken(token));
    }
}
