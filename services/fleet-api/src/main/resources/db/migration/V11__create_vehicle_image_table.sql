CREATE TABLE vehicle_image (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
 vehicle_id UUID NOT NULL,
 url VARCHAR(500) NOT NULL,
 public_id VARCHAR(255) NOT NULL,
 uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
 created_at TIMESTAMP NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
 CONSTRAINT fk_vehicle_image_vehicle
     FOREIGN KEY (vehicle_id)
         REFERENCES vehicle(id)
         ON DELETE CASCADE
);

COMMENT ON CONSTRAINT fk_vehicle_image_vehicle ON vehicle_image IS
    'CASCADE: vehicle images are compositionally tied to their vehicle; deleting the vehicle removes its images';

CREATE INDEX idx_vehicle_image_vehicle_id
    ON vehicle_image(vehicle_id);