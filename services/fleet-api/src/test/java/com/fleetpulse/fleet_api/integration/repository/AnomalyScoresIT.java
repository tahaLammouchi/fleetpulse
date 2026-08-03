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
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AnomalyScoresIT extends AbstractIntegrationTest {

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
                "WHERE hypertable_name = 'anomaly_scores'"
        );
        assertThat(hypertables).isNotEmpty();
    }

    @Test
    void shouldInsertAndQueryAnomalyScoresByTimeRange() {
        Vehicle vehicle = createVehicle();
        UUID vehicleId = vehicle.getId();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        OffsetDateTime base = OffsetDateTime.of(2025, 2, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        jdbc.update(
                "INSERT INTO anomaly_scores (time, vehicle_id, score, model_version) " +
                "VALUES (?, ?, ?, ?)",
                base, vehicleId, 0.85, "v1.0"
        );
        jdbc.update(
                "INSERT INTO anomaly_scores (time, vehicle_id, score, model_version) " +
                "VALUES (?, ?, ?, ?)",
                base.plusHours(1), vehicleId, 0.92, "v1.0"
        );
        jdbc.update(
                "INSERT INTO anomaly_scores (time, vehicle_id, score, model_version) " +
                "VALUES (?, ?, ?, ?)",
                base.plusDays(1), vehicleId, 0.45, "v1.0"
        );

        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM anomaly_scores " +
                "WHERE vehicle_id = ? AND time >= ? AND time <= ? " +
                "ORDER BY time ASC",
                vehicleId, base, base.plusHours(2)
        );
        assertThat(results).hasSize(2);
        assertThat((Double) results.get(0).get("score")).isCloseTo(0.85, within(0.001));
    }

    @Test
    void shouldEnforceScoreRangeCheckConstraint() {
        Vehicle vehicle = createVehicle();
        UUID vehicleId = vehicle.getId();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThrows(Exception.class, () -> jdbc.update(
                "INSERT INTO anomaly_scores (time, vehicle_id, score, model_version) " +
                "VALUES (?, ?, ?, ?)",
                OffsetDateTime.now(), vehicleId, 1.5, "v1.0"
        ));

        assertThrows(Exception.class, () -> jdbc.update(
                "INSERT INTO anomaly_scores (time, vehicle_id, score, model_version) " +
                "VALUES (?, ?, ?, ?)",
                OffsetDateTime.now(), vehicleId, -0.1, "v1.0"
        ));
    }

    @Test
    void shouldQueryByVehicleAndModelVersion() {
        Vehicle vehicle = createVehicle();
        UUID vehicleId = vehicle.getId();
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        OffsetDateTime base = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.update(
                "INSERT INTO anomaly_scores (time, vehicle_id, score, model_version) " +
                "VALUES (?, ?, ?, ?)",
                base, vehicleId, 0.7, "v1.0"
        );
        jdbc.update(
                "INSERT INTO anomaly_scores (time, vehicle_id, score, model_version) " +
                "VALUES (?, ?, ?, ?)",
                base.plusMinutes(30), vehicleId, 0.8, "v2.0"
        );

        List<Map<String, Object>> results = jdbc.queryForList(
                "SELECT * FROM anomaly_scores " +
                "WHERE vehicle_id = ? AND model_version = ? " +
                "ORDER BY time DESC",
                vehicleId, "v1.0"
        );
        assertThat(results).hasSize(1);
        assertThat((String) results.get(0).get("model_version")).isEqualTo("v1.0");
    }

    private Fleet createFleet() {
        Fleet fleet = new Fleet();
        fleet.setName("Anomaly Fleet");
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