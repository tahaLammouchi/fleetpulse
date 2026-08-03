package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.entity.AppUser;
import com.fleetpulse.fleet_api.domain.entity.Intervention;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.enums.AlertStatus;
import com.fleetpulse.fleet_api.domain.enums.InterventionStatus;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.InterventionRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import com.fleetpulse.fleet_api.specification.AlertSpecifications;
import com.fleetpulse.fleet_api.web.dto.response.AlertResponse;
import com.fleetpulse.fleet_api.web.mapper.AlertMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final InterventionRepository interventionRepository;
    private final AlertMapper alertMapper;

    public Page<AlertResponse> findAll(List<AlertStatus> statuses, UUID vehicleId, Pageable pageable) {
        Specification<Alert> spec = Specification
                .where(AlertSpecifications.statusIn(statuses))
                .and(AlertSpecifications.hasVehicle(vehicleId));
        return alertRepository.findAll(spec, pageable)
                .map(alertMapper::toResponse);
    }

    public AlertResponse findById(UUID id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        return alertMapper.toResponse(alert);
    }
    @Transactional
    public AlertResponse acknowledge(UUID id, AppUser currentUser) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        if (alert.getStatus() != AlertStatus.NEW) {
            throw new BusinessRuleViolationException(
                    "Alert is not in NEW status (current: " + alert.getStatus() + ")");
        }
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(currentUser);
        alert = alertRepository.save(alert);
        return alertMapper.toResponse(alert);
    }
    @Transactional
    public AlertResponse resolve(UUID id, AppUser currentUser) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        if (alert.getStatus() != AlertStatus.ACKNOWLEDGED) {
            throw new BusinessRuleViolationException(
                    "Alert must be ACKNOWLEDGED before resolving (current: " + alert.getStatus() + ")");
        }
        if (alert.getAcknowledgedBy() == null || !alert.getAcknowledgedBy().getId().equals(currentUser.getId())) {
            throw new BusinessRuleViolationException(
                    "Only the manager who acknowledged the alert can resolve it");
        }
        List<Intervention> relatedInterventions = interventionRepository.findAllByAlertId(alert.getId());
        boolean hasOpenInterventions = relatedInterventions.stream()
                .anyMatch(intervention -> !intervention.getStatus().equals(InterventionStatus.CLOSED));
        if (hasOpenInterventions) {
            throw new BusinessRuleViolationException(
                    "Cannot resolve alert: there are open interventions related to this alert");
        }
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert = alertRepository.save(alert);
        return alertMapper.toResponse(alert);
    }

    public Page<AlertResponse> findAcknowledgedByFleetManager(AppUser currentUser, List<AlertStatus> status, UUID vehicleId, Pageable pageable) {

        Specification<Alert> spec = Specification
                .where(AlertSpecifications.statusIn(status))
                .and(AlertSpecifications.hasVehicle(vehicleId))
                .and(AlertSpecifications.acknowledgedBy(currentUser.getKeycloakId()));
        return alertRepository.findAll(spec, pageable)
                .map(alertMapper::toResponse);
    }
}