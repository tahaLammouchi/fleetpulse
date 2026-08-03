package com.fleetpulse.fleet_api.integration.controller;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.integration.TestSecurityBeans;
import com.fleetpulse.fleet_api.repository.FleetRepository;
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

import java.util.UUID;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class FleetControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FleetRepository fleetRepository;

    private WebTestClient webTestClient;

    private UUID fleetId;

    @BeforeEach
    void initWebTestClient() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build();
    }

    @BeforeEach
    void setUp() {
        Fleet fleet = new Fleet();
        fleet.setName("Setup Fleet");
        fleet = fleetRepository.saveAndFlush(fleet);
        fleetId = fleet.getId();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateFleet() {
        webTestClient.post()
                .uri("/api/fleets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": \"Integration Fleet\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Integration Fleet")
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllFleets() {
        webTestClient.get()
                .uri("/api/fleets")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetFleetById() {
        webTestClient.get()
                .uri("/api/fleets/{id}", fleetId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Setup Fleet");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateFleet() {
        webTestClient.put()
                .uri("/api/fleets/{id}", fleetId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": \"Updated Fleet\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Updated Fleet");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteFleet() {
        webTestClient.delete()
                .uri("/api/fleets/{id}", fleetId)
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void shouldReturn401WhenUnauthenticated() {
        webTestClient.get()
                .uri("/api/fleets")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(roles = "FLEET_MANAGER")
    void shouldAllowFleetManagerToRead() {
        webTestClient.get()
                .uri("/api/fleets")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void shouldRejectTechnicianCreatingFleet() {
        webTestClient.post()
                .uri("/api/fleets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": \"Unauthorized\"}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectInvalidFleetName() {
        webTestClient.post()
                .uri("/api/fleets")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": \"\"}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}