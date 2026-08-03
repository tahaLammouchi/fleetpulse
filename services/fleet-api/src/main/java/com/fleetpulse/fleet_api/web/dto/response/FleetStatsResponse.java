package com.fleetpulse.fleet_api.web.dto.response;

import java.util.List;

public record FleetStatsResponse(
        long totalFleets,
        List<FleetVehicleCount> topFleets
) {
    public record FleetVehicleCount(String fleetName, long vehicleCount) {}
}