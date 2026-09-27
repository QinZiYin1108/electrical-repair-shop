package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserHomePublicResponse;
import com.example.backend.service.UserHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "公开/首页展示")
@RequestMapping("/pass/home")
public class HomePublicController {

    private final UserHomeService userHomeService;

    public HomePublicController(UserHomeService userHomeService) {
        this.userHomeService = userHomeService;
    }

    @Operation(summary = "查询公开首页Data")
    @GetMapping("/public")
    public Result<UserHomePublicResponse> getPublicHomeData() {
        return Result.success(userHomeService.getPublicHomeData());
    }
}
