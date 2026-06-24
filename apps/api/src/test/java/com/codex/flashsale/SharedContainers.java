package com.codex.flashsale;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Singleton container holder shared across all integration tests.
 * <p>
 * Uses a static initializer so MySQL, Redis and Kafka are started exactly once
 * per JVM — regardless of how many {@code @SpringBootTest} classes extend
 * {@link AbstractIntegrationTest}.  This avoids the per-class container
 * lifecycle that {@code @Testcontainers} + {@code @Container} creates for
 * static fields declared in an abstract superclass.
 */
final class SharedContainers {

    static final MySQLContainer<?> MYSQL;
    static final GenericContainer<?> REDIS;
    static final KafkaContainer KAFKA;

    static {
        MYSQL = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("flashsale")
                .withUsername("flashsale")
                .withPassword("flashsale");
        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4"))
                .withExposedPorts(6379);
        KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

        MYSQL.start();
        REDIS.start();
        KAFKA.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            KAFKA.stop();
            REDIS.stop();
            MYSQL.stop();
        }));
    }

    private SharedContainers() {
    }
}
