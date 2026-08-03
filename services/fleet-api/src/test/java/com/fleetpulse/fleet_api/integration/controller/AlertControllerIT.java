package com.fleetpulse.fleet_api.integration.controller;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import com.fleetpulse.fleet_api.domain.enums.UserRole;
import com.fleetpulse.fleet_api.domain.enums.UserStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.integration.TestSecurityBeans;
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.AppUserRepository;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import com.fleetpulse.fleet_api.security.CurrentUserResolver;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class AlertControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private CurrentUserResolver currentUserResolver;

    private WebTestClient webTestClient;

    private UUID alertId;
    private UUID vehicleId;
    private AppUser fleetManager;

    @BeforeEach
    void initWebTestClient() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build();
    }

    @BeforeEach
    void setUp() {
        Fleet fleet = new Fleet();
        fleet.setName("Alert Ctrl Fleet");
        fleet = fleetRepository.saveAndFlush(fleet);

        Vehicle vehicle = new Vehicle();
        vehicle.setFleet(fleet);
        vehicle.setLicensePlate("TST-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());
        vehicle = vehicleRepository.saveAndFlush(vehicle);
        vehicleId = vehicle.getId();

        fleetManager = new AppUser();
        fleetManager.setKeycloakId("fm-subject-001");
        fleetManager.setEmail("fm@test.com");
        fleetManager.setFullName("Fleet Manager");
        fleetManager.setRole(UserRole.FLEET_MANAGER);
        fleetManager.setStatus(UserStatus.ENABLED);
        fleetManager = appUserRepository.saveAndFlush(fleetManager);

        Alert alert = new Alert();
        alert.setVehicle(vehicle);
        alert.setAnomalyScoreValue(0.88);
        alert.setModelVersion("v1.0");
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setTriggeredAt(LocalDateTime.now());
        alert = alertRepository.saveAndFlush(alert);
        alertId = alert.getId();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListAlerts() {
        webTestClient.get()
                .uri("/api/alerts")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    @Test
    @WithMockUser(roles = "FLEET_MANAGER")
    void shouldGetAlertById() {
        webTestClient.get()
                .uri("/api/alerts/{id}", alertId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(alertId.toString());
    }

    @Test
    @WithMockUser(roles = "FLEET_MANAGER")
    void shouldCreateInterventionFromAlert() {
        webTestClient.post()
                .uri("/api/alerts/{alertId}/interventions", alertId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"description\": \"Investigate anomaly\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty();
    }

    @Test
    void shouldReturn401WhenUnauthenticated() {
        webTestClient.get()
                .uri("/api/alerts")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void shouldRejectTechnicianListingAlerts() {
        webTestClient.get()
                .uri("/api/alerts")
                .exchange()
                .expectStatus().isForbidden();
    }
}