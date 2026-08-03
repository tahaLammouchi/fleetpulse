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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TelemetryReadingsIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void shouldVerifyHypertableExists() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<Map<String, Object>> hypertables = jdbc.queryForList(
                "SELECT hypertable_name FROM timescaledb_information.hypertables " +
                "WHERE hypertable_name = 'telemetry_readings'"
        );
        assertThat(hypertables).isNotEmpty();
    }

    @Test
    void shouldInsertAndQueryTelemetryByTimeRange() {
        Vehicle vehicle = createVehicle();
        UUID vehicleId = vehicle.getId();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        OffsetDateTime base = OffsetDateTime.of(2025, 1, 10, 8, 0, 0, 0, ZoneOffset.UTC);
        jdbc.update(
                "INSERT INTO telemetry_readings (time, vehicle_id, temperature, vibration, rpm, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                base, vehicleId, 85.5, 0.02, 3200, 15000.0
        );
        jdbc.update(
                "INSERT INTO telemetry_readings (time, vehicle_id, temperature, vibration, rpm, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                base.plusHours(1), vehicleId, 86.0, 0.03, 3300, 15010.0
        );
        jdbc.update(
                "INSERT INTO telemetry_readings (time, vehicle_id, temperature, vibration, rpm, mileage) " +
                "VALUES (?, ?, ?, ?, ?, ?)",
                base.plusDays(2), vehicleId, 90.0, 0.05, 3400, 15200.0
        );

        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM telemetry_readings " +
                "WHERE vehicle_id = ? AND time >= ? AND time <= ? " +
                "ORDER BY time ASC",
                vehicleId, base, base.plusHours(2)
        );
        assertThat(results).hasSize(2);
        assertThat((Double) results.get(0).get("temperature")).isCloseTo(85.5, within(0.01));
    }

    @Test
    void shouldApplyCompressionPolicy() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        List<Map<String, Object>> policies = jdbc.queryForList(
                "SELECT * FROM timescaledb_information.jobs WHERE hypertable_name = 'telemetry_readings'"
        );
        assertThat(policies).isNotEmpty();
    }

    private Fleet createFleet() {
        Fleet fleet = new Fleet();
        fleet.setName("Telemetry Fleet");
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
}