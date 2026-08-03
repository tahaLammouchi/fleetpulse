package com.fleetpulse.fleet_api.integration.controller;

import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.integration.TestSecurityBeans;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class AlertThresholdControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void initWebTestClient() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_service")
    void shouldCreateAlertThreshold() {
        String body = """
                {"vehicleType": "TRUCK", "modelVersion": "v1.0", "thresholdValue": 0.85}
                """;

        webTestClient.post()
                .uri("/api/alert-thresholds")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.thresholdValue").isEqualTo(0.85)
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListAlertThresholds() {
        webTestClient.get()
                .uri("/api/alert-thresholds")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldFilterByVehicleType() {
        webTestClient.get()
                .uri("/api/alert-thresholds?vehicleType=TRUCK")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray();
    }

    @Test
    @WithMockUser(roles = "FLEET_MANAGER")
    void shouldRejectNonAdminListingThresholds() {
        webTestClient.get()
                .uri("/api/alert-thresholds")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectNonServiceCreatingThreshold() {
        webTestClient.post()
                .uri("/api/alert-thresholds")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"vehicleType": "CAR", "modelVersion": "v1.0", "thresholdValue": 0.7}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_service")
    void shouldRejectInvalidThreshold() {
        webTestClient.post()
                .uri("/api/alert-thresholds")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"modelVersion": "", "thresholdValue": null}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn401WhenUnauthenticated() {
        webTestClient.get()
                .uri("/api/alert-thresholds")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}