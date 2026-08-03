package com.fleetpulse.fleet_api.integration.repository;

import com.fleetpulse.fleet_api.domain.entity.AlertThreshold;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.repository.AlertThresholdRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AlertThresholdRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private AlertThresholdRepository alertThresholdRepository;

    @Test
    void shouldSaveAndFindThreshold() {
        AlertThreshold threshold = createThreshold(VehicleType.TRUCK, "v1.0", 0.8);
        assertThat(threshold.getId()).isNotNull();
        assertThat(threshold.getThresholdValue()).isEqualTo(0.8);
    }

    @Test
    void shouldFindByVehicleType() {
        createThreshold(VehicleType.TRUCK, "v1.0", 0.8);
        createThreshold(VehicleType.TRUCK, "v2.0", 0.9);

        List<AlertThreshold> results = alertThresholdRepository.findByVehicleType(VehicleType.TRUCK);
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindByModelVersion() {
        createThreshold(VehicleType.VAN, "v1.0", 0.7);
        createThreshold(VehicleType.TRUCK, "v1.0", 0.8);

        List<AlertThreshold> results = alertThresholdRepository.findByModelVersion("v1.0");
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindByVehicleTypeAndModelVersion() {
        createThreshold(VehicleType.CAR, "v1.5", 0.75);
        createThreshold(VehicleType.TRUCK, "v1.5", 0.85);

        List<AlertThreshold> results = alertThresholdRepository
                .findByVehicleTypeAndModelVersion(VehicleType.CAR, "v1.5");
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getThresholdValue()).isEqualTo(0.75);
    }

    @Test
    void shouldEnforceUniqueVehicleTypeAndModelVersion() {
        createThreshold(VehicleType.VAN, "v1.0", 0.7);

        AlertThreshold duplicate = new AlertThreshold();
        duplicate.setVehicleType(VehicleType.VAN);
        duplicate.setModelVersion("v1.0");
        duplicate.setThresholdValue(0.75);

        assertThrows(Exception.class,
                () -> alertThresholdRepository.saveAndFlush(duplicate));
    }

    @Test
    void shouldAllowNullVehicleTypeForGlobalThreshold() {
        createThreshold(null, "global-v1", 0.6);
        createThreshold(null, "global-v2", 0.65);

        List<AlertThreshold> global = alertThresholdRepository.findAll().stream()
                .filter(t -> t.getVehicleType() == null)
                .toList();
        assertThat(global).hasSize(2);
    }

    private AlertThreshold createThreshold(VehicleType vehicleType, String modelVersion, double value) {
        AlertThreshold threshold = new AlertThreshold();
        threshold.setVehicleType(vehicleType);
        threshold.setModelVersion(modelVersion);
        threshold.setThresholdValue(value);
        return alertThresholdRepository.saveAndFlush(threshold);
    }
}