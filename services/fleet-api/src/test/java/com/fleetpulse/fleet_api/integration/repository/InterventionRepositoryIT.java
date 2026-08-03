package com.fleetpulse.fleet_api.integration.repository;

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
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.AppUserRepository;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.InterventionRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class InterventionRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private InterventionRepository interventionRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindIntervention() {
        Vehicle vehicle = createVehicle();
        Intervention intervention = createIntervention(vehicle, null, null);

        assertThat(intervention.getId()).isNotNull();
        assertThat(intervention.getStatus()).isEqualTo(InterventionStatus.OPEN);
        assertThat(intervention.getVehicle().getId()).isEqualTo(vehicle.getId());
    }

    @Test
    void shouldCreateInterventionWithTechnicianAndAlert() {
        Vehicle vehicle = createVehicle();
        AppUser technician = createTechnician();
        Alert alert = createAlert(vehicle);
        Intervention intervention = createIntervention(vehicle, technician, alert);

        assertThat(intervention.getTechnician().getId()).isEqualTo(technician.getId());
        assertThat(intervention.getAlert().getId()).isEqualTo(alert.getId());
    }

    @Test
    void shouldCountByVehicleId() {
        Vehicle vehicle = createVehicle();
        createIntervention(vehicle, null, null);
        createIntervention(vehicle, null, null);

        assertThat(interventionRepository.countByVehicleId(vehicle.getId())).isEqualTo(2);
    }

    @Test
    void shouldFindAllByAlertId() {
        Vehicle vehicle = createVehicle();
        Alert alert = createAlert(vehicle);
        createIntervention(vehicle, null, alert);
        createIntervention(vehicle, null, alert);

        List<Intervention> results = interventionRepository.findAllByAlertId(alert.getId());
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldSetTechnicianToNullOnDelete() {
        Vehicle vehicle = createVehicle();
        AppUser technician = createTechnician();
        Intervention intervention = createIntervention(vehicle, technician, null);

        entityManager.clear();
        appUserRepository.deleteById(technician.getId());
        appUserRepository.flush();

        Intervention found = interventionRepository.findById(intervention.getId()).orElseThrow();
        assertThat(found.getTechnician()).isNull();
    }

    @Test
    void shouldCloseInterventionProperly() {
        Vehicle vehicle = createVehicle();
        AppUser technician = createTechnician();
        Intervention intervention = createIntervention(vehicle, technician, null);
        intervention.setStatus(InterventionStatus.IN_PROGRESS);
        interventionRepository.flush();

        intervention.close("Fixed the engine");
        interventionRepository.save(intervention);
        interventionRepository.flush();

        Intervention found = interventionRepository.findById(intervention.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(InterventionStatus.CLOSED);
        assertThat(found.getTechnicianReport()).isEqualTo("Fixed the engine");
        assertThat(found.getClosedAt()).isNotNull();
    }

    private Fleet createFleet() {
        Fleet fleet = new Fleet();
        fleet.setName("Intervention Fleet");
        return fleetRepository.saveAndFlush(fleet);
    }

    private Vehicle createVehicle() {
        Fleet fleet = createFleet();
        Vehicle vehicle = new Vehicle();
        vehicle.setFleet(fleet);
        vehicle.setLicensePlate("TST-" + UUID.randomUUID().toString().substring(0, 8));
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());
        return vehicleRepository.saveAndFlush(vehicle);
    }

    private AppUser createTechnician() {
        AppUser user = new AppUser();
        user.setKeycloakId("kc-tech-" + System.nanoTime());
        user.setEmail("tech@test.com");
        user.setFullName("Test Technician");
        user.setRole(UserRole.TECHNICIAN);
        user.setStatus(UserStatus.ENABLED);
        return appUserRepository.saveAndFlush(user);
    }

    private Alert createAlert(Vehicle vehicle) {
        Alert alert = new Alert();
        alert.setVehicle(vehicle);
        alert.setAnomalyScoreValue(0.85);
        alert.setModelVersion("v1.0");
        alert.setStatus(AlertStatus.NEW);
        alert.setTriggeredAt(LocalDateTime.now());
        return alertRepository.saveAndFlush(alert);
    }

    private Intervention createIntervention(Vehicle vehicle, AppUser technician, Alert alert) {
        Intervention intervention = new Intervention();
        intervention.setVehicle(vehicle);
        intervention.setTechnician(technician);
        intervention.setAlert(alert);
        intervention.setStatus(InterventionStatus.OPEN);
        intervention.setOpenedAt(LocalDateTime.now());
        return interventionRepository.saveAndFlush(intervention);
    }
}