package com.fleetpulse.fleet_api.integration;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ContextConfiguration(initializers = AbstractIntegrationTest.Initializer.class)
@Sql(scripts = "classpath:clean-db.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class AbstractIntegrationTest {


    static final PostgreSQLContainer<?> timescale =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("timescale/timescaledb:latest-pg16")
                            .asCompatibleSubstituteFor("postgres")
            ).withReuse(true);


    static {
        timescale.start();
    }


    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {


        public void initialize(ConfigurableApplicationContext context) {

            TestPropertyValues.of(
                            "spring.datasource.url=" + timescale.getJdbcUrl(),
                            "spring.datasource.username=" + timescale.getUsername(),
                            "spring.datasource.password=" + timescale.getPassword()
                    )
                    .applyTo(context.getEnvironment());
        }
    }
}