package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.InterventionRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import com.fleetpulse.fleet_api.specification.VehicleSpecifications;
import com.fleetpulse.fleet_api.web.dto.response.TelemetryReadingResponse;
import com.fleetpulse.fleet_api.web.dto.response.VehicleResponse;
import com.fleetpulse.fleet_api.web.dto.response.VehicleRestrictedResponse;
import com.fleetpulse.fleet_api.web.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final FleetRepository fleetRepository;
    private final AlertRepository alertRepository;
    private final InterventionRepository interventionRepository;
    private final VehicleMapper vehicleMapper;
    private final JdbcClient jdbcClient;

    @Transactional
    public VehicleResponse create(UUID fleetId, String licensePlate, String brand, String model, VehicleType vehicleType) {
        Fleet fleet = fleetRepository.findById(fleetId)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet", fleetId));
        if (vehicleRepository.existsByLicensePlate(licensePlate)) {
            throw new BusinessRuleViolationException("License plate already exists: " + licensePlate);
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setFleet(fleet);
        vehicle.setLicensePlate(licensePlate);
        vehicle.setBrand(brand);
        vehicle.setModel(model);
        vehicle.setVehicleType(vehicleType);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());
        vehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(vehicle);
    }

    public Page<VehicleResponse> findAll(String search, UUID fleetId, VehicleType vehicleType,
                                            String brand, String model,
                                          VehicleStatus status, Pageable pageable) {
        Specification<Vehicle> spec = Specification
                .where(VehicleSpecifications.plateContains(search))
                .and(VehicleSpecifications.hasFleet(fleetId))
                .and(VehicleSpecifications.hasVehicleType(vehicleType))
                .and(VehicleSpecifications.hasBrand(brand))
                .and(VehicleSpecifications.hasModel(model))
                .and(VehicleSpecifications.hasStatus(status));
        return vehicleRepository.findAll(spec, pageable)
                .map(vehicleMapper::toResponse);
    }

    public Object findById(UUID id, Boolean isTechnician) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        if (isTechnician) {
            return vehicleMapper.toRestrictedResponse(vehicle);
        }
        return vehicleMapper.toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse update(UUID id, String licensePlate, String brand, String model,
                                          VehicleType vehicleType, VehicleStatus status, UUID fleetId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        if (fleetId != null && !vehicle.getFleet().getId().equals(fleetId)) {
            Fleet fleet = fleetRepository.findById(fleetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Fleet", fleetId));
            vehicle.setFleet(fleet);
        }
        if (licensePlate != null && !licensePlate.equals(vehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlate(licensePlate)) {
                throw new BusinessRuleViolationException("License plate already exists: " + licensePlate);
            }
            vehicle.setLicensePlate(licensePlate);
        }
        if (brand != null) vehicle.setBrand(brand);
        if (model != null) vehicle.setModel(model);
        if (vehicleType != null) vehicle.setVehicleType(vehicleType);

        if (status != null && status != vehicle.getStatus()) {
            if (!isValidTransition(vehicle.getStatus(), status)) {
                throw new BusinessRuleViolationException(
                        "Invalid status transition from " + vehicle.getStatus() + " to " + status);
            }
            vehicle.setStatus(status);
        }
        vehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(vehicle);
    }

    @Transactional
    public VehicleResponse patchStatus(UUID id, VehicleStatus newStatus) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        VehicleStatus current = vehicle.getStatus();
        if (!isValidTransition(current, newStatus)) {
            throw new BusinessRuleViolationException(
                    "Invalid status transition from " + current + " to " + newStatus);
        }
        vehicle.setStatus(newStatus);
        vehicle = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(vehicle);
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        long alertCount = alertRepository.countByVehicleId(id);
        long interventionCount = interventionRepository.countByVehicleId(id);
        if (alertCount > 0 || interventionCount > 0) {
            throw new BusinessRuleViolationException(
                    "Cannot delete vehicle with " + alertCount + " alerts and "
                            + interventionCount + " interventions");
        }
        vehicleRepository.delete(vehicle);
    }

    public Page<TelemetryReadingResponse> findTelemetry(UUID vehicleId, OffsetDateTime from,
                                                         OffsetDateTime to, Pageable pageable) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle", vehicleId);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be before 'to'");
        }
        List<TelemetryReadingResponse> rows = jdbcClient.sql("""
                        SELECT time, vehicle_id, temperature, vibration, oil_level, rpm, mileage
                        FROM telemetry_readings
                        WHERE vehicle_id = :vehicleId
                          AND (:from IS NULL OR time >= :from::timestamptz)
                          AND (:to IS NULL OR time <= :to::timestamptz)
                        ORDER BY time DESC
                        LIMIT :limit OFFSET :offset
                        """)
                .param("vehicleId", vehicleId)
                .param("from", from)
                .param("to", to)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query((rs, rowNum) -> new TelemetryReadingResponse(
                        rs.getObject("time", OffsetDateTime.class),
                        UUID.fromString(rs.getString("vehicle_id")),
                        (Double) rs.getObject("temperature"),
                        (Double) rs.getObject("vibration"),
                        (Double) rs.getObject("oil_level"),
                        (Double) rs.getObject("rpm"),
                        (Double) rs.getObject("mileage")
                )).list();

        Long total = jdbcClient.sql("""
                        SELECT COUNT(*) FROM telemetry_readings
                        WHERE vehicle_id = :vehicleId
                          AND (:from IS NULL OR time >= :from::timestamptz)
                          AND (:to IS NULL OR time <= :to::timestamptz)
                        """)
                .param("vehicleId", vehicleId)
                .param("from", from)
                .param("to", to)
                .query(Long.class).single();

        return new PageImpl<>(rows, pageable, total);
    }

    public Map<VehicleType, Long> getStatsByType() {
        return vehicleRepository.findAll().stream()
                .collect(Collectors.groupingBy(Vehicle::getVehicleType, Collectors.counting()));
    }

    private boolean isValidTransition(VehicleStatus current, VehicleStatus next) {
        return switch (current) {
            case ACTIVE -> next == VehicleStatus.MAINTENANCE || next == VehicleStatus.DECOMMISSIONED;
            case MAINTENANCE -> next == VehicleStatus.ACTIVE || next == VehicleStatus.DECOMMISSIONED;
            case DECOMMISSIONED -> false;
        };
    }
}