package com.example.backend.controller.admin;

import com.example.backend.common.Result;
import com.example.backend.model.admin.AdminAnnouncementCreateRequest;
import com.example.backend.model.admin.AdminAnnouncementResponse;
import com.example.backend.model.admin.AdminAnnouncementUpdateRequest;
import com.example.backend.service.AdminAnnouncementService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/system/announcements")
public class AdminAnnouncementController {

    private final AdminAnnouncementService adminAnnouncementService;

    public AdminAnnouncementController(AdminAnnouncementService adminAnnouncementService) {
        this.adminAnnouncementService = adminAnnouncementService;
    }

    @GetMapping
    public Result<List<AdminAnnouncementResponse>> list(@RequestParam(value = "channel", required = false) Integer channel) {
        return Result.success(adminAnnouncementService.listAnnouncements(channel));
    }

    @PostMapping("/create")
    public Result<AdminAnnouncementResponse> create(@Valid @RequestBody AdminAnnouncementCreateRequest request) {
        return Result.success(adminAnnouncementService.createAnnouncement(request));
    }

    @PostMapping("/{id}/update")
    public Result<AdminAnnouncementResponse> update(
        @PathVariable("id") String id,
        @Valid @RequestBody AdminAnnouncementUpdateRequest request
    ) {
        return Result.success(adminAnnouncementService.updateAnnouncement(id, request));
    }

    @PostMapping("/{id}/delete")
    public Result<Void> delete(@PathVariable("id") String id) {
        adminAnnouncementService.deleteAnnouncement(id);
        return Result.success();
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadImage(@PathVariable("id") String id, @RequestPart("file") MultipartFile file) {
        return Result.success(adminAnnouncementService.uploadAnnouncementImage(id, file));
    }
}

