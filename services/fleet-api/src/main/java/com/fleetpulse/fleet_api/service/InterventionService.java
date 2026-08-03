package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.entity.Intervention;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import com.fleetpulse.fleet_api.domain.enums.InterventionStatus;
import com.fleetpulse.fleet_api.domain.enums.UserRole;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.AppUserRepository;
import com.fleetpulse.fleet_api.repository.InterventionRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import com.fleetpulse.fleet_api.specification.InterventionSpecifications;
import com.fleetpulse.fleet_api.web.dto.response.InterventionResponse;
import com.fleetpulse.fleet_api.web.mapper.InterventionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterventionService {

    private final InterventionRepository interventionRepository;
    private final VehicleRepository vehicleRepository;
    private final AlertRepository alertRepository;
    private final AppUserRepository appUserRepository;
    private final InterventionMapper interventionMapper;

    @Transactional
    public InterventionResponse create(UUID vehicleId, UUID technicianId, String description) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
        AppUser technician = null;
        if (technicianId != null) {
            technician = appUserRepository.findById(technicianId)
                    .orElseThrow(() -> new ResourceNotFoundException("Technician", technicianId));
        }
        Intervention intervention = new Intervention();
        intervention.setVehicle(vehicle);
        intervention.setTechnician(technician);
        intervention.setDescription(description);
        intervention.setStatus(InterventionStatus.OPEN);
        intervention.setOpenedAt(LocalDateTime.now());
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.save(vehicle);
        intervention = interventionRepository.save(intervention);
        return interventionMapper.toResponse(intervention);
    }
    @Transactional
    public InterventionResponse createFromAlert(UUID alertId, UUID technicianId, String description) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));
        AppUser technician = null;
        if (technicianId != null) {
            technician = appUserRepository.findById(technicianId)
                    .orElseThrow(() -> new ResourceNotFoundException("Technician", technicianId));
        }
        if (alert.getStatus() == AlertStatus.NEW) {
            throw new BusinessRuleViolationException(
                    "Cannot create intervention: alert is still NEW, must be acknowledged first");
        }
        Vehicle vehicle = alert.getVehicle();
        Intervention intervention = new Intervention();
        intervention.setVehicle(alert.getVehicle());
        intervention.setAlert(alert);
        intervention.setTechnician(technician);
        intervention.setDescription(description);
        intervention.setStatus(InterventionStatus.OPEN);
        intervention.setOpenedAt(LocalDateTime.now());
        intervention = interventionRepository.save(intervention);
        vehicle.setStatus(VehicleStatus.MAINTENANCE);
        vehicleRepository.save(vehicle);
        return interventionMapper.toResponse(intervention);
    }

    public Page<InterventionResponse> findAll(InterventionStatus status, UUID vehicleId,
                                               UUID technicianId, Pageable pageable) {
        Specification<Intervention> spec = Specification
                .where(InterventionSpecifications.hasStatus(status))
                .and(InterventionSpecifications.hasVehicle(vehicleId))
                .and(InterventionSpecifications.hasTechnician(technicianId));
        return interventionRepository.findAll(spec, pageable)
                .map(interventionMapper::toResponse);
    }
    public Page<InterventionResponse> findAssignedToMe(UUID technicianId, InterventionStatus status,
                                                        Pageable pageable) {
        Specification<Intervention> spec = Specification
                .where(InterventionSpecifications.assignedTo(technicianId))
                .and(InterventionSpecifications.hasStatus(status));
        return interventionRepository.findAll(spec, pageable)
                .map(interventionMapper::toResponse);
    }
    public InterventionResponse findById(UUID id) {
        Intervention intervention = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
        return interventionMapper.toResponse(intervention);
    }
    @Transactional
    public InterventionResponse assign(UUID id, UUID technicianId) {
        Intervention intervention = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
        AppUser technician = appUserRepository.findById(technicianId)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", technicianId));
        if (technician.getRole() != UserRole.TECHNICIAN) {
            throw new BusinessRuleViolationException(
                    "User " + technicianId + " does not have TECHNICIAN role");
        }
        intervention.setTechnician(technician);
        intervention = interventionRepository.save(intervention);
        return interventionMapper.toResponse(intervention);
    }
    @Transactional
    public InterventionResponse start(UUID id, AppUser currentUser) {
        Intervention intervention = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
        if (intervention.getTechnician() == null
                || !intervention.getTechnician().getId().equals(currentUser.getId())) {
            throw new BusinessRuleViolationException("Intervention not assigned to you");
        }
        if (intervention.getStatus() != InterventionStatus.OPEN) {
            throw new BusinessRuleViolationException(
                    "Intervention must be OPEN to start (current: " + intervention.getStatus() + ")");
        }
        intervention.setStatus(InterventionStatus.IN_PROGRESS);
        intervention = interventionRepository.save(intervention);
        return interventionMapper.toResponse(intervention);
    }
    @Transactional
    public InterventionResponse close(UUID id, String technicianReport, AppUser currentUser) {
        Intervention intervention = interventionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intervention", id));
        if (intervention.getTechnician() == null
                || !intervention.getTechnician().getId().equals(currentUser.getId())) {
            throw new BusinessRuleViolationException("Intervention must be closed by the assigned technician");
        }
        intervention.close(technicianReport);
        intervention = interventionRepository.save(intervention);
        List<Intervention> vehicleInterventions = interventionRepository.findByVehicleId(intervention.getVehicle().getId());
        boolean hasInProgressInterventions = vehicleInterventions.stream()
                .anyMatch(i -> i.getStatus() == InterventionStatus.IN_PROGRESS);
        if (!hasInProgressInterventions) {
            Vehicle vehicle = intervention.getVehicle();
            vehicle.setStatus(VehicleStatus.ACTIVE);
            vehicleRepository.save(vehicle);
        }
        return interventionMapper.toResponse(intervention);
    }
    public Page<InterventionResponse> findAllByAlert(UUID alertId, InterventionStatus status, UUID technicianId, Pageable pageable) {
        Specification<Intervention> spec = Specification
                .where(InterventionSpecifications.hasAlert(alertId))
                .and(InterventionSpecifications.hasStatus(status))
                .and(InterventionSpecifications.hasTechnician(technicianId));
        return interventionRepository.findAll(spec, pageable)
                .map(interventionMapper::toResponse);
    }
}