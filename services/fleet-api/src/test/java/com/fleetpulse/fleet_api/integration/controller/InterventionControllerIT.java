package com.fleetpulse.fleet_api.integration.controller;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.domain.entity.Intervention;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import com.fleetpulse.fleet_api.domain.enums.InterventionStatus;
import com.fleetpulse.fleet_api.domain.enums.UserRole;
import com.fleetpulse.fleet_api.domain.enums.UserStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.integration.TestSecurityBeans;
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.AppUserRepository;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.InterventionRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

@AutoConfigureMockMvc
@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class InterventionControllerIT extends AbstractIntegrationTest {

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
    private InterventionRepository interventionRepository;

    private WebTestClient webTestClient;

    private UUID vehicleId;
    private UUID interventionId;

    @BeforeEach
    void initWebTestClient() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc).build();
    }

    @BeforeEach
    void setUp() {
        Fleet fleet = new Fleet();
        fleet.setName("Int Ctrl Fleet");
        fleet = fleetRepository.saveAndFlush(fleet);

        Vehicle vehicle = new Vehicle();
        vehicle.setFleet(fleet);
        vehicle.setLicensePlate("TST-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());
        vehicle = vehicleRepository.saveAndFlush(vehicle);
        vehicleId = vehicle.getId();

        Intervention intervention = new Intervention();
        intervention.setVehicle(vehicle);
        intervention.setStatus(InterventionStatus.OPEN);
        intervention.setOpenedAt(LocalDateTime.now());
        intervention = interventionRepository.saveAndFlush(intervention);
        interventionId = intervention.getId();
    }

    @Test
    @WithMockUser(roles = "FLEET_MANAGER")
    void shouldCreateIntervention() {
        webTestClient.post()
                .uri("/api/interventions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"vehicleId\": \"" + vehicleId + "\", \"description\": \"Engine check\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.status").isEqualTo("OPEN");
    }

    @Test
    @WithMockUser(roles = "FLEET_MANAGER")
    void shouldGetInterventions() {
        webTestClient.get()
                .uri("/api/interventions")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content").isArray();
    }

    @Test
    void shouldReturn401WhenUnauthenticated() {
        webTestClient.get()
                .uri("/api/interventions")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void shouldRejectTechnicianCreatingIntervention() {
        webTestClient.post()
                .uri("/api/interventions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"vehicleId\": \"" + vehicleId + "\", \"description\": \"No access\"}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToRead() {
        webTestClient.get()
                .uri("/api/interventions")
                .exchange()
                .expectStatus().isOk();
    }
}