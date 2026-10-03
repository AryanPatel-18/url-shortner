package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.InfrastructureMetricsResponse;

public interface IInfrastructureAdminService {
    InfrastructureMetricsResponse getInfrastructureMetrics();
}