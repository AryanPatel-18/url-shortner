package com.aryan.url_shortner.controller;

import com.aryan.url_shortner.dto.AdminBusinessStatisticsResponse;
import com.aryan.url_shortner.dto.AdminTestResponse;
import com.aryan.url_shortner.dto.InfrastructureMetricsResponse;
import com.aryan.url_shortner.dto.SystemMetricsResponse;
import com.aryan.url_shortner.service.admin.IAdminService;
import com.aryan.url_shortner.service.admin.IInfrastructureAdminService;
import com.aryan.url_shortner.service.admin.ISystemAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/${api.version}/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ISystemAdminService systemAdminService;
    private final IAdminService adminService;
    private final IInfrastructureAdminService infrastructureAdminService;

    @GetMapping("/test")

    public ResponseEntity<AdminTestResponse> adminTest() {
        return ResponseEntity.ok(adminService.getTestStatus());
    }

    @GetMapping("/business")
    public ResponseEntity<AdminBusinessStatisticsResponse> getBusinessStatistics() {
        return ResponseEntity.ok(adminService.getBusinessStatistics());
    }

    @GetMapping("/system")
    public ResponseEntity<SystemMetricsResponse> getSystemMetrics() {
        return ResponseEntity.ok(systemAdminService.getSystemMetrics());
    }

    @GetMapping("/infrastructure")
    public ResponseEntity<InfrastructureMetricsResponse> getInfrastructureMetrics() {
        return ResponseEntity.ok(infrastructureAdminService.getInfrastructureMetrics());
    }

}