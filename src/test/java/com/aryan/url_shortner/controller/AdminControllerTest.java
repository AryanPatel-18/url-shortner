package com.aryan.url_shortner.controller;

import com.aryan.url_shortner.dto.AdminDashboardResponse;
import com.aryan.url_shortner.service.admin.IAdminService;
import com.aryan.url_shortner.service.admin.IDashboardAdminService;
import com.aryan.url_shortner.service.admin.IInfrastructureAdminService;
import com.aryan.url_shortner.service.admin.ISystemAdminService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
class AdminControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @MockitoBean
    private IDashboardAdminService dashboardAdminService;
    
    @MockitoBean
    private ISystemAdminService systemAdminService;
    
    @MockitoBean
    private IAdminService adminService;
    
    @MockitoBean
    private IInfrastructureAdminService infrastructureAdminService;

    @Value("${api.version}")
    private String apiVersion;

    @Test
    void getDashboard_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/" + apiVersion + "/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getDashboard_WithUserRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/" + apiVersion + "/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getDashboard_WithAdminRole_Returns200AndValidStructure() throws Exception {
        AdminDashboardResponse mockResponse = new AdminDashboardResponse(
                new AdminDashboardResponse.ApplicationSection("PT1H", Instant.now(), "UP"),
                new AdminDashboardResponse.HttpSection(100, 95, 4, 1, 0, 10.0, 50.0, 100.0),
                new AdminDashboardResponse.DatabaseSection(2, 8, 0, 10, 2, 0),
                new AdminDashboardResponse.RedisSection("UP", 5, 1000, 1024, 2048, 50, 10),
                new AdminDashboardResponse.JvmSection(500, 1000, 200, 10, 0.05, 5, 100.0),
                new AdminDashboardResponse.BusinessSection(10, 2, 8, 100, 90, 10, 500),
                new AdminDashboardResponse.WorkerSection(10, 0, 5.0)
        );

        Mockito.when(dashboardAdminService.getDashboard()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/" + apiVersion + "/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application.status").value("UP"))
                .andExpect(jsonPath("$.http.totalRequests").value(100))
                .andExpect(jsonPath("$.database.activeConnections").value(2))
                .andExpect(jsonPath("$.redis.status").value("UP"))
                .andExpect(jsonPath("$.jvm.liveThreads").value(10))
                .andExpect(jsonPath("$.business.totalUsers").value(10))
                .andExpect(jsonPath("$.workers.clickFlushFailures").value(0));
    }
}
