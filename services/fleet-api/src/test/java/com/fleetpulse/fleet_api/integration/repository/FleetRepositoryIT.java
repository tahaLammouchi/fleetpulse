package com.fleetpulse.fleet_api.integration.repository;

import com.fleetpulse.fleet_api.domain.entity.Fleet;
import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import com.fleetpulse.fleet_api.repository.FleetRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class FleetRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private FleetRepository fleetRepository;

    @Test
    void shouldSaveAndFindFleet() {
        Fleet fleet = new Fleet();
        fleet.setName("Logistics Fleet");

        Fleet saved = fleetRepository.save(fleet);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Logistics Fleet");
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Fleet> found = fleetRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Logistics Fleet");
    }

    @Test
    void shouldFindAllFleets() {
        Fleet fleet1 = new Fleet();
        fleet1.setName("Fleet Alpha");
        fleetRepository.save(fleet1);

        Fleet fleet2 = new Fleet();
        fleet2.setName("Fleet Beta");
        fleetRepository.save(fleet2);

        assertThat(fleetRepository.findAll()).hasSize(2);
    }

    @Test
    void shouldCountFleets() {
        fleetRepository.save(createFleet("Count Test Fleet"));
        assertThat(fleetRepository.count()).isPositive();
    }

    @Test
    void shouldEnforceNotNullName() {
        Fleet fleet = new Fleet();
        assertThrows(Exception.class,
                () -> fleetRepository.saveAndFlush(fleet));
    }

    private Fleet createFleet(String name) {
        Fleet fleet = new Fleet();
        fleet.setName(name);
        return fleetRepository.save(fleet);
    }
}