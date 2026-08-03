package com.fleetpulse.fleet_api.integration.controller;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.integration.TestSecurityBeans;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
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
class VehicleControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private WebTestClient webTestClient;

    private UUID fleetId;

    @BeforeEach
    void initWebTestClient() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build();
    }

    @BeforeEach
    void setUp() {
        Fleet fleet = new Fleet();
        fleet.setName("Controller Test Fleet");
        fleet = fleetRepository.saveAndFlush(fleet);
        fleetId = fleet.getId();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateVehicle() {
        webTestClient.post()
                .uri("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"fleetId": "%s", "licensePlate": "CTRL-001",
                         "brand": "Ford", "model": "Transit", "vehicleType": "VAN"}
                        """.formatted(fleetId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.licensePlate").isEqualTo("CTRL-001")
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllVehicles() {
        webTestClient.get()
                .uri("/api/vehicles")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    @Test
    void shouldReturn401WhenUnauthenticated() {
        webTestClient.get()
                .uri("/api/vehicles")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(roles = "FLEET_MANAGER")
    void shouldAllowFleetManagerToRead() {
        webTestClient.get()
                .uri("/api/vehicles")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void shouldRejectTechnicianCreatingVehicle() {
        webTestClient.post()
                .uri("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"fleetId\": \"" + fleetId + "\", \"licensePlate\": \"NO-TECH\", \"vehicleType\": \"CAR\"}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectInvalidVehicle() {
        webTestClient.post()
                .uri("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"fleetId\": null, \"licensePlate\": \"\", \"vehicleType\": null}")
                .exchange()
                .expectStatus().isBadRequest();
    }
}