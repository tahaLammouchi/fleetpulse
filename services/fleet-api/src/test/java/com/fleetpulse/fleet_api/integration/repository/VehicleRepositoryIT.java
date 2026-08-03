package com.fleetpulse.fleet_api.integration.repository;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VehicleRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FleetRepository fleetRepository;

    @Test
    void shouldSaveAndFindVehicle() {
        Fleet fleet = createFleet("Vehicle Fleet");
        Vehicle vehicle = createVehicle(fleet, "AB-123-CD", VehicleType.CAR);

        assertThat(vehicle.getId()).isNotNull();
        assertThat(vehicle.getLicensePlate()).isEqualTo("AB-123-CD");
        assertThat(vehicle.getFleet().getId()).isEqualTo(fleet.getId());
    }

    @Test
    void shouldEnforceUniqueLicensePlate() {
        Fleet fleet = createFleet("Unique Plate Fleet");
        createVehicle(fleet, "UNIQUE-01", VehicleType.TRUCK);

        Vehicle duplicate = new Vehicle();
        duplicate.setFleet(fleet);
        duplicate.setLicensePlate("UNIQUE-01");
        duplicate.setVehicleType(VehicleType.TRUCK);
        duplicate.setStatus(VehicleStatus.ACTIVE);
        duplicate.setRegisteredAt(LocalDateTime.now());

        assertThrows(Exception.class, () -> vehicleRepository.saveAndFlush(duplicate));
    }

    @Test
    void shouldEnforceForeignKeyConstraint() {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate("NO-FLEET-01");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());

        assertThrows(Exception.class, () -> vehicleRepository.saveAndFlush(vehicle));
    }

    @Test
    void shouldCountVehiclesByFleetId() {
        Fleet fleet = createFleet("Count Fleet");
        createVehicle(fleet, "C-001", VehicleType.VAN);
        createVehicle(fleet, "C-002", VehicleType.TRUCK);

        assertThat(vehicleRepository.countByFleetId(fleet.getId())).isEqualTo(2);
    }

    @Test
    void shouldCheckLicensePlateExists() {
        Fleet fleet = createFleet("Exists Fleet");
        createVehicle(fleet, "EXISTS-01", VehicleType.CAR);

        assertThat(vehicleRepository.existsByLicensePlate("EXISTS-01")).isTrue();
        assertThat(vehicleRepository.existsByLicensePlate("NONEXISTENT")).isFalse();
    }

    @Test
    void shouldFindVehiclesByFleetIdPaginated() {
        Fleet fleet = createFleet("Paginated Fleet");
        for (int i = 0; i < 5; i++) {
            createVehicle(fleet, "PAG-" + i, VehicleType.CAR);
        }

        assertThat(vehicleRepository.findByFleetId(fleet.getId(), PageRequest.of(0, 3)))
                .hasSize(3);
        assertThat(vehicleRepository.findByFleetId(fleet.getId(), PageRequest.of(1, 3)))
                .hasSize(2);
    }

    @Test
    void shouldCreateVehicleWithAllFields() {
        Fleet fleet = createFleet("Full Fields Fleet");
        Vehicle vehicle = new Vehicle();
        vehicle.setFleet(fleet);
        vehicle.setLicensePlate("FULL-001");
        vehicle.setBrand("Renault");
        vehicle.setModel("Master");
        vehicle.setVehicleType(VehicleType.VAN);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.of(2025, 1, 15, 10, 0));

        Vehicle saved = vehicleRepository.saveAndFlush(vehicle);
        assertThat(saved.getBrand()).isEqualTo("Renault");
        assertThat(saved.getModel()).isEqualTo("Master");
        assertThat(saved.getStatus()).isEqualTo(VehicleStatus.ACTIVE);
    }

    private Fleet createFleet(String name) {
        Fleet fleet = new Fleet();
        fleet.setName(name);
        return fleetRepository.saveAndFlush(fleet);
    }

    private Vehicle createVehicle(Fleet fleet, String plate, VehicleType type) {
        Vehicle vehicle = new Vehicle();
        vehicle.setFleet(fleet);
        vehicle.setLicensePlate(plate);
        vehicle.setVehicleType(type);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());
        return vehicleRepository.saveAndFlush(vehicle);
    }
}