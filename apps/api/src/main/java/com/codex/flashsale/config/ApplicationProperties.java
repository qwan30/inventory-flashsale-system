package com.codex.flashsale.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private final Reservation reservation = new Reservation();
    private final Lock lock = new Lock();
    private final Outbox outbox = new Outbox();
    private final Scheduler scheduler = new Scheduler();
    private final Kafka kafka = new Kafka();

    public Reservation getReservation() {
        return reservation;
    }

    public Lock getLock() {
        return lock;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public static class Reservation {
        private Duration ttl = Duration.ofMinutes(10);

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static class Lock {
        private Duration waitTimeout = Duration.ofSeconds(2);
        private Duration leaseTimeout = Duration.ofSeconds(5);

        public Duration getWaitTimeout() {
            return waitTimeout;
        }

        public void setWaitTimeout(Duration waitTimeout) {
            this.waitTimeout = waitTimeout;
        }

        public Duration getLeaseTimeout() {
            return leaseTimeout;
        }

        public void setLeaseTimeout(Duration leaseTimeout) {
            this.leaseTimeout = leaseTimeout;
        }
    }

    public static class Outbox {
        private int publishBatchSize = 50;
        private Duration retryDelay = Duration.ofSeconds(10);
        private int maxAttempts = 5;

        public int getPublishBatchSize() {
            return publishBatchSize;
        }

        public void setPublishBatchSize(int publishBatchSize) {
            this.publishBatchSize = publishBatchSize;
        }

        public Duration getRetryDelay() {
            return retryDelay;
        }

        public void setRetryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class Scheduler {
        private Duration expiredReservationDelay = Duration.ofSeconds(30);
        private Duration outboxDelay = Duration.ofSeconds(5);

        public Duration getExpiredReservationDelay() {
            return expiredReservationDelay;
        }

        public void setExpiredReservationDelay(Duration expiredReservationDelay) {
            this.expiredReservationDelay = expiredReservationDelay;
        }

        public Duration getOutboxDelay() {
            return outboxDelay;
        }

        public void setOutboxDelay(Duration outboxDelay) {
            this.outboxDelay = outboxDelay;
        }
    }

    public static class Kafka {
        private String topic = "inventory-flashsale.events";

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }
}
