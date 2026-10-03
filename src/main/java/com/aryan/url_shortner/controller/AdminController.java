package com.aryan.url_shortner.controller;

import com.aryan.url_shortner.dto.AdminTestResponse;
import com.aryan.url_shortner.service.admin.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/${api.version}/admin")
@RequiredArgsConstructor
public class AdminController {
    private final IAdminService adminService;

    @GetMapping("/test")

    public ResponseEntity<AdminTestResponse> adminTest() {
        return ResponseEntity.ok(adminService.getTestStatus());
    }
}