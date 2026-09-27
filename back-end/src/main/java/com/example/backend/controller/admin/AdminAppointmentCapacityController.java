package com.example.backend.controller.admin;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.Result;
import com.example.backend.entity.AppointmentClosures;
import com.example.backend.entity.AppointmentReservations;
import com.example.backend.exception.BusinessException;
import com.example.backend.model.appointment.AppointmentClosureCreateRequest;
import com.example.backend.security.context.AuthUserContext;
import com.example.backend.security.model.LoginUserInfo;
import com.example.backend.service.AppointmentCapacityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员端/预约容量")
@RestController
@RequestMapping("/admin/appointment-capacity")
public class AdminAppointmentCapacityController {

    private final AppointmentCapacityService appointmentCapacityService;

    public AdminAppointmentCapacityController(
            AppointmentCapacityService appointmentCapacityService) {
        this.appointmentCapacityService = appointmentCapacityService;
    }

    private LoginUserInfo requireAdmin() {
        LoginUserInfo user = AuthUserContext.get();
        if (user == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "需登录管理员");
        }
        return user;
    }

    @Operation(summary = "新增停业/请假时段")
    @PostMapping("/closures")
    public Result<AppointmentClosures> createClosure(
            @RequestBody(required = false) AppointmentClosureCreateRequest request) {
        LoginUserInfo admin = requireAdmin();
        return Result.success(
                appointmentCapacityService.createClosure(request, admin.getAccountId()));
    }

    @Operation(summary = "查询停业/请假时段")
    @GetMapping("/closures")
    public Result<List<AppointmentClosures>> listClosures(
            @RequestParam(value = "ownerType", required = false) String ownerType,
            @RequestParam(value = "ownerId", required = false) String ownerId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireAdmin();
        return Result.success(appointmentCapacityService.listClosures(ownerType, ownerId, limit));
    }

    @Operation(summary = "删除停业/请假时段")
    @PostMapping("/closures/{id}/delete")
    public Result<Integer> deleteClosure(@PathVariable("id") String id) {
        requireAdmin();
        return Result.success(appointmentCapacityService.deleteClosure(id));
    }

    @Operation(summary = "查询师傅时段占用")
    @GetMapping("/reservations")
    public Result<List<AppointmentReservations>> listReservations(
            @RequestParam(value = "technicianAccountId", required = false)
                    String technicianAccountId,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        requireAdmin();
        return Result.success(
                appointmentCapacityService.listReservations(technicianAccountId, limit));
    }
}
