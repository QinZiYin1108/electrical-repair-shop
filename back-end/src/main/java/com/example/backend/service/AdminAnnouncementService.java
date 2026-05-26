package com.example.backend.service;

import com.example.backend.model.admin.AdminAnnouncementCreateRequest;
import com.example.backend.model.admin.AdminAnnouncementResponse;
import com.example.backend.model.admin.AdminAnnouncementUpdateRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AdminAnnouncementService {

    List<AdminAnnouncementResponse> listAnnouncements(Integer channel);

    AdminAnnouncementResponse createAnnouncement(AdminAnnouncementCreateRequest request);

    AdminAnnouncementResponse updateAnnouncement(String id, AdminAnnouncementUpdateRequest request);

    void deleteAnnouncement(String id);

    String uploadAnnouncementImage(String id, MultipartFile file);
}

