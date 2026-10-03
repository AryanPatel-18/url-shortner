package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.SystemMetricsResponse;

public interface ISystemAdminService {
    SystemMetricsResponse getSystemMetrics();
}