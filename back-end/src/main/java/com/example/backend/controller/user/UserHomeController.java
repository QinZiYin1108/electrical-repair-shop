package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserHomePrivateResponse;
import com.example.backend.service.UserHomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/home")
public class UserHomeController {

    private final UserHomeService userHomeService;

    public UserHomeController(UserHomeService userHomeService) {
        this.userHomeService = userHomeService;
    }

    @GetMapping("/private")
    public Result<UserHomePrivateResponse> getCurrentUserHomePrivateData() {
        return Result.success(userHomeService.getCurrentUserPrivateHomeData());
    }
}

