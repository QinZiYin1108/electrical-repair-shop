package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.model.admin.AdminProtocolItemResponse;
import com.example.backend.service.ProtocolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Tag(name = "管理员端/协议管理")
@RequestMapping("/admin/system/settings/protocols")
public class AdminProtocolController {

    private final ProtocolService protocolService;

    public AdminProtocolController(ProtocolService protocolService) {
        this.protocolService = protocolService;
    }

    @GetMapping
    public Result<List<AdminProtocolItemResponse>> listProtocols() {
        return Result.success(protocolService.listProtocols());
    }

    @Operation(summary = "上传上传Protocol")
    @PostMapping(value = "/{type}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AdminProtocolItemResponse> uploadProtocol(
            @PathVariable("type") String type, @RequestPart("file") MultipartFile file) {
        return Result.success(protocolService.uploadProtocol(type, file));
    }
}
