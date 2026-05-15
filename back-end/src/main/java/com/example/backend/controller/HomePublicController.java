package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserHomePublicResponse;
import com.example.backend.service.UserHomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pass/home")
public class HomePublicController {

    private final UserHomeService userHomeService;

    public HomePublicController(UserHomeService userHomeService) {
        this.userHomeService = userHomeService;
    }

    @GetMapping("/public")
    public Result<UserHomePublicResponse> getPublicHomeData() {
        return Result.success(userHomeService.getPublicHomeData());
    }
}

