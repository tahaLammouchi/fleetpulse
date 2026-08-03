package com.fleetpulse.fleet_api;

import com.fleetpulse.fleet_api.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class FleetApiApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}
}