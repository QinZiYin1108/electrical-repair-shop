package com.example.backend.controller;

import com.example.backend.common.Result;
import com.example.backend.model.common.ProtocolContentResponse;
import com.example.backend.service.ProtocolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pass/protocols")
public class ProtocolPublicController {

    private final ProtocolService protocolService;

    public ProtocolPublicController(ProtocolService protocolService) {
        this.protocolService = protocolService;
    }

    @GetMapping("/{type}")
    public Result<ProtocolContentResponse> getProtocol(@PathVariable("type") String type) {
        return Result.success(protocolService.getProtocolContent(type));
    }
}
