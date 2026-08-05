package com.fleetpulse.fleet_api.exception;

import java.io.IOException;

public class CloudinaryException extends RuntimeException {
    public CloudinaryException(String message) {
        super(message);
    }
}
