package com.codex.flashsale.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private final Reservation reservation = new Reservation();
    private final Lock lock = new Lock();
    private final Outbox outbox = new Outbox();
    private final Channel channel = new Channel();
    private final Alerts alerts = new Alerts();
    private final Scheduler scheduler = new Scheduler();
    private final Kafka kafka = new Kafka();
    private final Security security = new Security();

    public Reservation getReservation() {
        return reservation;
    }

    public Lock getLock() {
        return lock;
    }

    public Outbox getOutbox() {
        return outbox;
    }

    public Channel getChannel() {
        return channel;
    }

    public Alerts getAlerts() {
        return alerts;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public Security getSecurity() {
        return security;
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

    public static class Channel {
        private int syncBatchSize = 50;
        private Duration retryDelay = Duration.ofSeconds(15);
        private int maxAttempts = 3;
        private final Shopee shopee = new Shopee();

        public int getSyncBatchSize() {
            return syncBatchSize;
        }

        public void setSyncBatchSize(int syncBatchSize) {
            this.syncBatchSize = syncBatchSize;
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

        public Shopee getShopee() {
            return shopee;
        }
    }

    public static class Shopee {
        private String mode = "mock";
        private String baseUrl = "https://partner.test-stable.shopeemobile.com";
        private Long partnerId;
        private String partnerKey;
        private Long shopId;
        private String accessToken;
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Long getPartnerId() {
            return partnerId;
        }

        public void setPartnerId(Long partnerId) {
            this.partnerId = partnerId;
        }

        public String getPartnerKey() {
            return partnerKey;
        }

        public void setPartnerKey(String partnerKey) {
            this.partnerKey = partnerKey;
        }

        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class Alerts {
        private long outboxFailedThreshold = 10;
        private long channelSyncFailedThreshold = 10;
        private long reconciliationOpenDriftThreshold = 5;
        private Duration channelSnapshotStaleness = Duration.ofMinutes(5);
        private final AlertDelivery delivery = new AlertDelivery();

        public long getOutboxFailedThreshold() {
            return outboxFailedThreshold;
        }

        public void setOutboxFailedThreshold(long outboxFailedThreshold) {
            this.outboxFailedThreshold = outboxFailedThreshold;
        }

        public long getChannelSyncFailedThreshold() {
            return channelSyncFailedThreshold;
        }

        public void setChannelSyncFailedThreshold(long channelSyncFailedThreshold) {
            this.channelSyncFailedThreshold = channelSyncFailedThreshold;
        }

        public long getReconciliationOpenDriftThreshold() {
            return reconciliationOpenDriftThreshold;
        }

        public void setReconciliationOpenDriftThreshold(long reconciliationOpenDriftThreshold) {
            this.reconciliationOpenDriftThreshold = reconciliationOpenDriftThreshold;
        }

        public Duration getChannelSnapshotStaleness() {
            return channelSnapshotStaleness;
        }

        public void setChannelSnapshotStaleness(Duration channelSnapshotStaleness) {
            this.channelSnapshotStaleness = channelSnapshotStaleness;
        }

        public AlertDelivery getDelivery() {
            return delivery;
        }
    }

    public static class AlertDelivery {
        private boolean enabled;
        private String webhookUrl;
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);
        private Duration reminderInterval = Duration.ofMinutes(15);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Duration getReminderInterval() {
            return reminderInterval;
        }

        public void setReminderInterval(Duration reminderInterval) {
            this.reminderInterval = reminderInterval;
        }
    }

    public static class Scheduler {
        private Duration expiredReservationDelay = Duration.ofSeconds(30);
        private Duration outboxDelay = Duration.ofSeconds(5);
        private Duration channelSyncDelay = Duration.ofSeconds(10);
        private Duration reconciliationDelay = Duration.ofSeconds(60);
        private Duration alertDeliveryDelay = Duration.ofSeconds(30);

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

        public Duration getChannelSyncDelay() {
            return channelSyncDelay;
        }

        public void setChannelSyncDelay(Duration channelSyncDelay) {
            this.channelSyncDelay = channelSyncDelay;
        }

        public Duration getReconciliationDelay() {
            return reconciliationDelay;
        }

        public void setReconciliationDelay(Duration reconciliationDelay) {
            this.reconciliationDelay = reconciliationDelay;
        }

        public Duration getAlertDeliveryDelay() {
            return alertDeliveryDelay;
        }

        public void setAlertDeliveryDelay(Duration alertDeliveryDelay) {
            this.alertDeliveryDelay = alertDeliveryDelay;
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

    public static class Security {
        private final Jwt jwt = new Jwt();
        private final SeedUsers seedUsers = new SeedUsers();

        public Jwt getJwt() {
            return jwt;
        }

        public SeedUsers getSeedUsers() {
            return seedUsers;
        }
    }

    public static class Jwt {
        private String issuer = "inventory-flashsale-api";
        private String secret = "change-me-change-me-change-me-1234";
        private Duration accessTokenTtl = Duration.ofMinutes(15);
        private Duration refreshTokenTtl = Duration.ofDays(7);

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getAccessTokenTtl() {
            return accessTokenTtl;
        }

        public void setAccessTokenTtl(Duration accessTokenTtl) {
            this.accessTokenTtl = accessTokenTtl;
        }

        public Duration getRefreshTokenTtl() {
            return refreshTokenTtl;
        }

        public void setRefreshTokenTtl(Duration refreshTokenTtl) {
            this.refreshTokenTtl = refreshTokenTtl;
        }
    }

    public static class SeedUsers {
        private String adminUsername = "admin";
        private String adminPassword = "Admin123!";
        private String adminDisplayName = "System Admin";
        private String operatorUsername = "operator";
        private String operatorPassword = "Operator123!";
        private String operatorDisplayName = "Operations User";

        public String getAdminUsername() {
            return adminUsername;
        }

        public void setAdminUsername(String adminUsername) {
            this.adminUsername = adminUsername;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public String getAdminDisplayName() {
            return adminDisplayName;
        }

        public void setAdminDisplayName(String adminDisplayName) {
            this.adminDisplayName = adminDisplayName;
        }

        public String getOperatorUsername() {
            return operatorUsername;
        }

        public void setOperatorUsername(String operatorUsername) {
            this.operatorUsername = operatorUsername;
        }

        public String getOperatorPassword() {
            return operatorPassword;
        }

        public void setOperatorPassword(String operatorPassword) {
            this.operatorPassword = operatorPassword;
        }

        public String getOperatorDisplayName() {
            return operatorDisplayName;
        }

        public void setOperatorDisplayName(String operatorDisplayName) {
            this.operatorDisplayName = operatorDisplayName;
        }
    }
}
