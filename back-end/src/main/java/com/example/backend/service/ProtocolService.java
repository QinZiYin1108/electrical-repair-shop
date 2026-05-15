package com.example.backend.service;

import com.example.backend.model.admin.AdminProtocolItemResponse;
import com.example.backend.model.common.ProtocolContentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProtocolService {

    List<AdminProtocolItemResponse> listProtocols();

    AdminProtocolItemResponse uploadProtocol(String type, MultipartFile file);

    ProtocolContentResponse getProtocolContent(String type);
}
