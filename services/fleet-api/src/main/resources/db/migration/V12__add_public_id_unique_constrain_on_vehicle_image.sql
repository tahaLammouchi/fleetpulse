ALTER TABLE vehicle_image
    ADD CONSTRAINT uk_vehicle_image_public_id
        UNIQUE (public_id);