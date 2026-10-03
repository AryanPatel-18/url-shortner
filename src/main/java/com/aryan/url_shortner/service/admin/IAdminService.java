package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.AdminBusinessStatisticsResponse;
import com.aryan.url_shortner.dto.AdminTestResponse;

public interface IAdminService {
    AdminTestResponse getTestStatus();
    AdminBusinessStatisticsResponse getBusinessStatistics();
}