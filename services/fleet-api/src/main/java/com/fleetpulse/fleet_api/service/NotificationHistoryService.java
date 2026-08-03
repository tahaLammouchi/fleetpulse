package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.Alert;
import com.fleetpulse.fleet_api.domain.entity.NotificationHistory;
import com.fleetpulse.fleet_api.domain.enums.NotificationChannel;
import com.fleetpulse.fleet_api.domain.enums.NotificationStatus;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.AlertRepository;
import com.fleetpulse.fleet_api.repository.NotificationHistoryRepository;
import com.fleetpulse.fleet_api.specification.NotificationHistorySpecifications;
import com.fleetpulse.fleet_api.web.dto.response.NotificationHistoryResponse;
import com.fleetpulse.fleet_api.web.mapper.NotificationHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationHistoryService {

    private final NotificationHistoryRepository repository;
    private final AlertRepository alertRepository;
    private final NotificationHistoryMapper mapper;

    public Page<NotificationHistoryResponse> findAll(UUID alertId, NotificationStatus status,
                                                      Pageable pageable) {
        Specification<NotificationHistory> spec = Specification
                .where(NotificationHistorySpecifications.hasAlert(alertId))
                .and(NotificationHistorySpecifications.hasStatus(status));
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional
    public NotificationHistoryResponse create(UUID alertId, String sentTo,
                                               NotificationChannel channel, NotificationStatus status) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", alertId));
        NotificationHistory nh = new NotificationHistory();
        nh.setAlert(alert);
        nh.setSentTo(sentTo);
        nh.setChannel(channel);
        nh.setStatus(status);
        nh.setSentAt(LocalDateTime.now());
        nh = repository.save(nh);
        return mapper.toResponse(nh);
    }
}