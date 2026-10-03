package com.aryan.url_shortner.service.admin;

import com.aryan.url_shortner.dto.AdminTestResponse;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AdminService implements IAdminService {

    @Override
    public AdminTestResponse getTestStatus() {
        return new AdminTestResponse(
                "Admin API Foundation is active and secure.",
                LocalDateTime.now()
        );
    }
}