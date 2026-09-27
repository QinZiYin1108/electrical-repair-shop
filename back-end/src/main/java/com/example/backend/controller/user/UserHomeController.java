package com.example.backend.controller.user;

import com.example.backend.common.Result;
import com.example.backend.model.user.UserHomePrivateResponse;
import com.example.backend.service.UserHomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "用户端/首页")
@RequestMapping("/user/home")
public class UserHomeController {

    private final UserHomeService userHomeService;

    public UserHomeController(UserHomeService userHomeService) {
        this.userHomeService = userHomeService;
    }

    @Operation(summary = "查询CurrentUser首页个人中心Data")
    @GetMapping("/private")
    public Result<UserHomePrivateResponse> getCurrentUserHomePrivateData() {
        return Result.success(userHomeService.getCurrentUserPrivateHomeData());
    }
}
