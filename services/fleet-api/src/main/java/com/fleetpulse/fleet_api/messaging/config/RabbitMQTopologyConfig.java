package com.fleetpulse.fleet_api.messaging.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQTopologyConfig {

    // ===== Exchange "telemetry" =====
    public static final String TELEMETRY_EXCHANGE = "telemetry";
    public static final String TELEMETRY_DLX = "telemetry.dlx";

    @Bean
    public TopicExchange telemetryExchange() {
        return new TopicExchange(TELEMETRY_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange telemetryDlx() {
        return new TopicExchange(TELEMETRY_DLX, true, false);
    }

    @Bean
    public Queue fleetApiTelemetryWriteQueue() {
        return QueueBuilder.durable("fleet-api.telemetry.write")
                .withArgument("x-dead-letter-exchange", TELEMETRY_DLX)
                .withArgument("x-dead-letter-routing-key", "fleet-api.telemetry.write.dlq")
                .build();
    }

    @Bean
    public Queue fleetApiTelemetryWriteDlq() {
        return QueueBuilder.durable("fleet-api.telemetry.write.dlq").build();
    }

    @Bean
    public Binding fleetApiTelemetryBinding() {
        return BindingBuilder.bind(fleetApiTelemetryWriteQueue())
                .to(telemetryExchange())
                .with("vehicle.*");
    }

    @Bean
    public Binding fleetApiTelemetryDlqBinding() {
        return BindingBuilder.bind(fleetApiTelemetryWriteDlq())
                .to(telemetryDlx())
                .with("fleet-api.telemetry.write.dlq");
    }

    // ===== Exchange "anomaly.scores" =====
    public static final String ANOMALY_SCORES_EXCHANGE = "anomaly.scores";
    public static final String ANOMALY_SCORES_DLX = "anomaly-scores.dlx";

    @Bean
    public TopicExchange anomalyScoresExchange() {
        return new TopicExchange(ANOMALY_SCORES_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange anomalyScoresDlx() {
        return new TopicExchange(ANOMALY_SCORES_DLX, true, false);
    }

    @Bean
    public Queue fleetApiScoresWriteQueue() {
        return QueueBuilder.durable("fleet-api.scores.write")
                .withArgument("x-dead-letter-exchange", ANOMALY_SCORES_DLX)
                .withArgument("x-dead-letter-routing-key", "fleet-api.scores.write.dlq")
                .build();
    }

    @Bean
    public Queue fleetApiScoresWriteDlq() {
        return QueueBuilder.durable("fleet-api.scores.write.dlq").build();
    }

    @Bean
    public Binding fleetApiScoresBinding() {
        return BindingBuilder.bind(fleetApiScoresWriteQueue())
                .to(anomalyScoresExchange())
                .with("vehicle.*");
    }

    @Bean
    public Binding fleetApiScoresDlqBinding() {
        return BindingBuilder.bind(fleetApiScoresWriteDlq())
                .to(anomalyScoresDlx())
                .with("fleet-api.scores.write.dlq");
    }
}