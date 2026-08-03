package com.fleetpulse.fleet_api.integration.repository;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AlertRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindAlert() {
        Vehicle vehicle = createVehicle();
        Alert alert = createAlert(vehicle, AlertStatus.NEW);

        assertThat(alert.getId()).isNotNull();
        assertThat(alert.getStatus()).isEqualTo(AlertStatus.NEW);
        assertThat(alert.getVehicle().getId()).isEqualTo(vehicle.getId());
    }

    @Test
    void shouldCountByVehicleId() {
        Vehicle vehicle = createVehicle();
        createAlert(vehicle, AlertStatus.NEW);
        createAlert(vehicle, AlertStatus.ACKNOWLEDGED);

        assertThat(alertRepository.countByVehicleId(vehicle.getId())).isEqualTo(2);
    }

    @Test
    void shouldUpdateAlertStatus() {
        Vehicle vehicle = createVehicle();
        Alert alert = createAlert(vehicle, AlertStatus.NEW);

        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alertRepository.saveAndFlush(alert);

        Alert updated = alertRepository.findById(alert.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(updated.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void shouldResolveAlert() {
        Vehicle vehicle = createVehicle();
        Alert alert = createAlert(vehicle, AlertStatus.ACKNOWLEDGED);

        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alertRepository.saveAndFlush(alert);

        Alert resolved = alertRepository.findById(alert.getId()).orElseThrow();
        assertThat(resolved.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    }

    @Test
    void shouldEnforceForeignKeyToVehicle() {
        Alert alert = new Alert();
        alert.setAnomalyScoreValue(0.9);
        alert.setModelVersion("v1.0");
        alert.setStatus(AlertStatus.NEW);
        alert.setTriggeredAt(LocalDateTime.now());

        assertThrows(DataIntegrityViolationException.class,
                () -> alertRepository.saveAndFlush(alert));
    }

    @Test
    void shouldNotDeleteVehicleWhenAlertsExist() {
        Vehicle vehicle = createVehicle();
        createAlert(vehicle, AlertStatus.NEW);

        assertThatThrownBy(() -> {
            entityManager.clear();
            vehicleRepository.delete(vehicle);
            vehicleRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Fleet createFleet() {
        Fleet fleet = new Fleet();
        fleet.setName("Alert Fleet");
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

    private Alert createAlert(Vehicle vehicle, AlertStatus status) {
        Alert alert = new Alert();
        alert.setVehicle(vehicle);
        alert.setAnomalyScoreValue(0.75);
        alert.setModelVersion("v1.0");
        alert.setStatus(status);
        alert.setTriggeredAt(LocalDateTime.now());
        return alertRepository.saveAndFlush(alert);
    }
}