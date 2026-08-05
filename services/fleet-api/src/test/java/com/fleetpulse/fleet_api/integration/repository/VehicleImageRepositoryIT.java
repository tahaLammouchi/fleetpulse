package com.fleetpulse.fleet_api.integration.repository;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.domain.entity.Vehicle;
import com.fleetpulse.fleet_api.domain.entity.VehicleImage;
import com.fleetpulse.fleet_api.domain.enums.VehicleStatus;
import com.fleetpulse.fleet_api.domain.enums.VehicleType;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import com.fleetpulse.fleet_api.repository.VehicleImageRepository;
import com.fleetpulse.fleet_api.repository.VehicleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class VehicleImageRepositoryIT extends AbstractIntegrationTest {


    @Autowired
    private VehicleImageRepository vehicleImageRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private FleetRepository fleetRepository;

    @Autowired
    private EntityManager entityManager;


    @Test
    void shouldSaveAndFindVehicleImage() {

        Fleet fleet = createFleet("Image Test Fleet");
        Vehicle vehicle = createVehicle(
                fleet,
                "IMG-001",
                VehicleType.CAR
        );


        VehicleImage image = createImage(
                vehicle,
                "https://cloudinary.com/car.jpg",
                "fleetpulse/vehicles/car1"
        );


        assertThat(image.getId()).isNotNull();
        assertThat(image.getVehicle().getId())
                .isEqualTo(vehicle.getId());

        assertThat(image.getUrl())
                .isEqualTo("https://cloudinary.com/car.jpg");
    }



    @Test
    void shouldFindImagesByVehicleId() {

        Fleet fleet = createFleet("Images Fleet");

        Vehicle vehicle = createVehicle(
                fleet,
                "IMG-002",
                VehicleType.TRUCK
        );


        createImage(
                vehicle,
                "url1",
                "public-id-1"
        );

        createImage(
                vehicle,
                "url2",
                "public-id-2"
        );


        List<VehicleImage> images =
                vehicleImageRepository.findByVehicleId(vehicle.getId());


        assertThat(images)
                .hasSize(2);

        assertThat(images)
                .extracting(VehicleImage::getPublicId)
                .containsExactlyInAnyOrder(
                        "public-id-1",
                        "public-id-2"
                );
    }



    @Test
    void shouldCountImagesByVehicleId() {

        Fleet fleet = createFleet("Count Images Fleet");

        Vehicle vehicle = createVehicle(
                fleet,
                "IMG-003",
                VehicleType.VAN
        );


        createImage(vehicle, "url1", "id1");
        createImage(vehicle, "url2", "id2");
        createImage(vehicle, "url3", "id3");


        long count =
                vehicleImageRepository.countByVehicleId(
                        vehicle.getId()
                );


        assertThat(count)
                .isEqualTo(3);
    }



    @Test
    void shouldDeleteImagesByVehicle() {

        Fleet fleet = createFleet("Cascade Fleet");

        Vehicle vehicle = createVehicle(
                fleet,
                "IMG-004",
                VehicleType.CAR
        );


        createImage(
                vehicle,
                "url",
                "public-id"
        );


        assertThat(
                vehicleImageRepository.countByVehicleId(
                        vehicle.getId()
                )
        ).isEqualTo(1);


        entityManager.clear();
        vehicleRepository.delete(vehicle);
        vehicleRepository.flush();


        assertThat(
                vehicleImageRepository.count()
        ).isEqualTo(0);
    }



    @Test
    void shouldFailWhenVehicleDoesNotExist() {

        VehicleImage image = new VehicleImage();

        image.setUrl("fake-url");
        image.setPublicId("fake-public-id");
        image.setUploadedAt(LocalDateTime.now());


        assertThrows(Exception.class, () ->
                vehicleImageRepository.saveAndFlush(image)
        );
    }




    private Fleet createFleet(String name) {

        Fleet fleet = new Fleet();
        fleet.setName(name);

        return fleetRepository.saveAndFlush(fleet);
    }



    private Vehicle createVehicle(
            Fleet fleet,
            String plate,
            VehicleType type
    ) {

        Vehicle vehicle = new Vehicle();

        vehicle.setFleet(fleet);
        vehicle.setLicensePlate(plate);
        vehicle.setVehicleType(type);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        vehicle.setRegisteredAt(LocalDateTime.now());


        return vehicleRepository.saveAndFlush(vehicle);
    }



    private VehicleImage createImage(
            Vehicle vehicle,
            String url,
            String publicId
    ) {

        VehicleImage image = new VehicleImage();

        image.setVehicle(vehicle);
        image.setUrl(url);
        image.setPublicId(publicId);
        image.setUploadedAt(LocalDateTime.now());


        return vehicleImageRepository.saveAndFlush(image);
    }

}