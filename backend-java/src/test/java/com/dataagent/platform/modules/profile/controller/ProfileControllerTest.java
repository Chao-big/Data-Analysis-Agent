package com.dataagent.platform.modules.profile.controller;

import com.dataagent.platform.common.security.AuthenticatedUserPrincipal;
import com.dataagent.platform.common.security.BearerTokenAuthenticationFilter;
import com.dataagent.platform.common.security.SecurityAccessDeniedHandler;
import com.dataagent.platform.common.security.SecurityAuthenticationEntryPoint;
import com.dataagent.platform.common.security.SecurityConfig;
import com.dataagent.platform.modules.auth.service.AuthService;
import com.dataagent.platform.modules.profile.domain.dto.ProfileResponse;
import com.dataagent.platform.modules.profile.domain.dto.ProfileUpdateRequest;
import com.dataagent.platform.modules.profile.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({
        SecurityConfig.class,
        BearerTokenAuthenticationFilter.class,
        SecurityAuthenticationEntryPoint.class,
        SecurityAccessDeniedHandler.class
})
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private ProfileService profileService;

    @Test
    void currentProfileShouldReturnUserProfile() throws Exception {
        when(authService.authenticate("access-token")).thenReturn(Optional.of(principal()));
        when(profileService.getCurrentProfile(any())).thenReturn(profileResponse());

        mockMvc.perform(get("/api/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value("1"))
                .andExpect(jsonPath("$.data.nickname").value("Analyst A"))
                .andExpect(jsonPath("$.data.email").value("analyst@example.com"))
                .andExpect(jsonPath("$.data.gender").value(1));
    }

    @Test
    void currentProfileShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/profile/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void updateProfileShouldReturnUpdatedProfile() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest(
                "Analyst B",
                "https://static.local/avatar/analyst-b.png",
                "analyst-b@example.com",
                "13800000009",
                (short) 0
        );

        when(authService.authenticate("access-token")).thenReturn(Optional.of(principal()));
        when(profileService.updateCurrentProfile(any(), any())).thenReturn(new ProfileResponse(
                "1",
                "analyst01",
                "Analyst B",
                "https://static.local/avatar/analyst-b.png",
                "analyst-b@example.com",
                "13800000009",
                (short) 0,
                "ACTIVE",
                LocalDateTime.of(2026, 8, 29, 10, 0),
                "203.0.113.10",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 29, 10, 5)
        ));

        mockMvc.perform(put("/api/profile/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("Analyst B"))
                .andExpect(jsonPath("$.data.phone").value("13800000009"))
                .andExpect(jsonPath("$.data.gender").value(0));
    }

    private AuthenticatedUserPrincipal principal() {
        return new AuthenticatedUserPrincipal(
                "access-token",
                "1",
                "analyst01",
                "Analyst A",
                "https://static.local/avatar/analyst-a.png",
                "ACTIVE",
                "tenant-demo",
                Set.of("ANALYST"),
                Set.of("dataset-sales"),
                Set.of("phone")
        );
    }

    private ProfileResponse profileResponse() {
        return new ProfileResponse(
                "1",
                "analyst01",
                "Analyst A",
                "https://static.local/avatar/analyst-a.png",
                "analyst@example.com",
                "13800000001",
                (short) 1,
                "ACTIVE",
                LocalDateTime.of(2026, 8, 29, 10, 0),
                "203.0.113.10",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 29, 10, 5)
        );
    }
}
