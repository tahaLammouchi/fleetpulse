package com.fleetpulse.fleet_api.service;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.exception.BusinessRuleViolationException;
import com.fleetpulse.fleet_api.exception.ResourceNotFoundException;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import com.fleetpulse.fleet_api.specification.FleetSpecifications;
import com.fleetpulse.fleet_api.web.dto.response.FleetResponse;
import com.fleetpulse.fleet_api.web.dto.response.FleetStatsResponse;
import com.fleetpulse.fleet_api.web.dto.response.VehicleResponse;
import com.fleetpulse.fleet_api.web.mapper.FleetMapper;
import com.fleetpulse.fleet_api.web.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FleetService {

    private final FleetRepository fleetRepository;
    private final VehicleRepository vehicleRepository;
    private final FleetMapper fleetMapper;
    private final VehicleMapper vehicleMapper;

    @Transactional
    public FleetResponse create(String name) {
        Fleet fleet = new Fleet();
        fleet.setName(name);
        fleet = fleetRepository.save(fleet);
        return fleetMapper.toResponseWithCount(fleet, 0);
    }

    public Page<FleetResponse> findAll(String search, Pageable pageable) {
        Specification<Fleet> spec = Specification.where(FleetSpecifications.nameContains(search));
        return fleetRepository.findAll(spec, pageable)
                .map(f -> fleetMapper.toResponseWithCount(f, vehicleRepository.countByFleetId(f.getId())));
    }

    public FleetResponse findById(UUID id) {
        Fleet fleet = fleetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet", id));
        return fleetMapper.toResponseWithCount(fleet, vehicleRepository.countByFleetId(fleet.getId()));
    }

    @Transactional
    public FleetResponse update(UUID id, String name) {
        Fleet fleet = fleetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet", id));
        fleet.setName(name);
        fleet = fleetRepository.save(fleet);
        return fleetMapper.toResponseWithCount(fleet, vehicleRepository.countByFleetId(fleet.getId()));
    }

    @Transactional
    public void delete(UUID id) {
        Fleet fleet = fleetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fleet", id));
        long vehicleCount = vehicleRepository.countByFleetId(id);
        if (vehicleCount > 0) {
            throw new BusinessRuleViolationException(
                    "Cannot delete fleet with " + vehicleCount + " vehicles attached");
        }
        fleetRepository.delete(fleet);
    }

    public Page<VehicleResponse> findVehiclesByFleetId(UUID fleetId, Pageable pageable) {
        if (!fleetRepository.existsById(fleetId)) {
            throw new ResourceNotFoundException("Fleet", fleetId);
        }
        return vehicleRepository.findByFleetId(fleetId, pageable)
                .map(vehicleMapper::toResponse);
    }

    public FleetStatsResponse getStats() {
        long totalFleets = fleetRepository.count();
        List<FleetStatsResponse.FleetVehicleCount> top = fleetRepository.findAll().stream()
                .map(f -> new FleetStatsResponse.FleetVehicleCount(
                        f.getName(), vehicleRepository.countByFleetId(f.getId())))
                .sorted((a, b) -> Long.compare(b.vehicleCount(), a.vehicleCount()))
                .limit(10)
                .toList();
        return new FleetStatsResponse(totalFleets, top);
    }
}