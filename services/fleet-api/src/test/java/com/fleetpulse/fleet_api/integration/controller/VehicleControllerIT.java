package com.fleetpulse.fleet_api.integration.controller;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.entity.VehicleImage;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.integration.TestSecurityBeans;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.VehicleImageRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import com.fleetpulse.fleet_api.service.CloudinaryService;
import com.fleetpulse.fleet_api.web.dto.response.UploadSignatureResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Autowired
    private VehicleImageRepository vehicleImageRepository;

    @MockitoBean
    private CloudinaryService cloudinaryService;

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGenerateUploadSignature() {
        Vehicle vehicle = createVehicle("SIG-001");
        String folder = "fleetpulse/vehicles/" + vehicle.getId();

        when(cloudinaryService.generateSignature(vehicle.getId()))
                .thenReturn(new UploadSignatureResponse("test-signature", 123456789L,
                        "test-api-key", "test-cloud", folder));

        webTestClient.post()
                .uri("/api/vehicles/{id}/images/upload-signature", vehicle.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.signature").isEqualTo("test-signature")
                .jsonPath("$.timestamp").isEqualTo(123456789L)
                .jsonPath("$.apiKey").isEqualTo("test-api-key")
                .jsonPath("$.cloudName").isEqualTo("test-cloud")
                .jsonPath("$.folder").isEqualTo(folder);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSaveVehicleImages() {
        Vehicle vehicle = createVehicle("IMG-001");

        webTestClient.post()
                .uri("/api/vehicles/{id}/images", vehicle.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"images": [{"url": "https://res.cloudinary.com/demo/car1.jpg",
                                     "publicId": "fleetpulse/vehicles/%s/car1"}]}
                        """.formatted(vehicle.getId()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$[0].id").isNotEmpty()
                .jsonPath("$[0].url").isEqualTo("https://res.cloudinary.com/demo/car1.jpg");

        assertThat(vehicleImageRepository.countByVehicleId(vehicle.getId())).isEqualTo(1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenVehicleNotFoundForImageUpload() {
        webTestClient.post()
                .uri("/api/vehicles/{id}/images", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"images": [{"url": "https://res.cloudinary.com/demo/x.jpg",
                                     "publicId": "fleetpulse/vehicles/x/x"}]}
                        """)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRejectMoreThanFiveImages() {
        Vehicle vehicle = createVehicle("IMG-002");
        for (int i = 0; i < 5; i++) {
            createImage(vehicle, "https://res.cloudinary.com/demo/" + i + ".jpg", "public-id-" + i);
        }

        webTestClient.post()
                .uri("/api/vehicles/{id}/images", vehicle.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"images": [{"url": "https://res.cloudinary.com/demo/6.jpg",
                                     "publicId": "public-id-6"}]}
                        """)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetVehicleImages() {
        Vehicle vehicle = createVehicle("IMG-003");
        createImage(vehicle, "https://res.cloudinary.com/demo/a.jpg", "public-id-a");
        createImage(vehicle, "https://res.cloudinary.com/demo/b.jpg", "public-id-b");

        webTestClient.get()
                .uri("/api/vehicles/{id}/images", vehicle.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteVehicleImages() {
        Vehicle vehicle = createVehicle("IMG-004");
        VehicleImage image = createImage(vehicle, "https://res.cloudinary.com/demo/c.jpg", "public-id-c");

        webTestClient.method(HttpMethod.DELETE)
                .uri("/api/vehicles/{id}/images", vehicle.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"imageIds": ["%s"]}
                        """.formatted(image.getId()))
                .exchange()
                .expectStatus().isNoContent();

        assertThat(vehicleImageRepository.countByVehicleId(vehicle.getId())).isZero();
        verify(cloudinaryService).deleteImage("public-id-c");
    }

    @Test
    void shouldReturn401WhenUnauthenticatedForImages() {
        webTestClient.get()
                .uri("/api/vehicles/{id}/images", UUID.randomUUID())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void shouldReturn403WhenRoleInsufficientForImages() {
        webTestClient.post()
                .uri("/api/vehicles/{id}/images", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"images": [{"url": "https://res.cloudinary.com/demo/x.jpg",
                                     "publicId": "x"}]}
                        """)
                .exchange()
                .expectStatus().isForbidden();
    }

    private Vehicle createVehicle(String plate) {
        Vehicle vehicle = new Vehicle();
        vehicle.setFleet(fleetRepository.findById(fleetId).orElseThrow());
        vehicle.setLicensePlate(plate);
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());
        return vehicleRepository.saveAndFlush(vehicle);
    }

    private VehicleImage createImage(Vehicle vehicle, String url, String publicId) {
        VehicleImage image = new VehicleImage();
        image.setVehicle(vehicle);
        image.setUrl(url);
        image.setPublicId(publicId);
        image.setUploadedAt(LocalDateTime.now());
        return vehicleImageRepository.saveAndFlush(image);
    }
}
