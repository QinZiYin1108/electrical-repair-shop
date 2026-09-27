package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.model.common.ProtocolContentResponse;
import com.example.backend.service.ProtocolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "公开/协议查看")
@RequestMapping("/pass/protocols")
public class ProtocolPublicController {

    private final ProtocolService protocolService;

    public ProtocolPublicController(ProtocolService protocolService) {
        this.protocolService = protocolService;
    }

    @Operation(summary = "查询Protocol")
    @GetMapping("/{type}")
    public Result<ProtocolContentResponse> getProtocol(@PathVariable("type") String type) {
        return Result.success(protocolService.getProtocolContent(type));
    }
}
