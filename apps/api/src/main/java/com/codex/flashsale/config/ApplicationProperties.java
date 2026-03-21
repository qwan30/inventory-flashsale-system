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
    private final Benchmark benchmark = new Benchmark();
    private final Ai ai = new Ai();
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

    public Benchmark getBenchmark() {
        return benchmark;
    }

    public Ai getAi() {
        return ai;
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
        private final TikTok tikTok = new TikTok();

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

        public TikTok getTikTok() {
            return tikTok;
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

    public static class TikTok {
        private String mode = "mock";
        private String baseUrl = "https://open-api.tiktokglobalshop.com";
        private String appKey;
        private String appSecret;
        private String shopCipher;
        private String accessToken;
        private String ingressSecret;
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

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getShopCipher() {
            return shopCipher;
        }

        public void setShopCipher(String shopCipher) {
            this.shopCipher = shopCipher;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getIngressSecret() {
            return ingressSecret;
        }

        public void setIngressSecret(String ingressSecret) {
            this.ingressSecret = ingressSecret;
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
        private final Slack slack = new Slack();
        private final PagerDuty pagerDuty = new PagerDuty();

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

        public Slack getSlack() {
            return slack;
        }

        public PagerDuty getPagerDuty() {
            return pagerDuty;
        }
    }

    public static class Slack {
        private boolean enabled;
        private String webhookUrl;
        private String minimumSeverity = "WARN";
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);

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

        public String getMinimumSeverity() {
            return minimumSeverity;
        }

        public void setMinimumSeverity(String minimumSeverity) {
            this.minimumSeverity = minimumSeverity;
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

    public static class PagerDuty {
        private boolean enabled;
        private String eventsUrl = "https://events.pagerduty.com/v2/enqueue";
        private String routingKey;
        private String minimumSeverity = "CRITICAL";
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getEventsUrl() {
            return eventsUrl;
        }

        public void setEventsUrl(String eventsUrl) {
            this.eventsUrl = eventsUrl;
        }

        public String getRoutingKey() {
            return routingKey;
        }

        public void setRoutingKey(String routingKey) {
            this.routingKey = routingKey;
        }

        public String getMinimumSeverity() {
            return minimumSeverity;
        }

        public void setMinimumSeverity(String minimumSeverity) {
            this.minimumSeverity = minimumSeverity;
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

    public static class Benchmark {
        private String evidenceRoot = "testing/k6/evidence";

        public String getEvidenceRoot() {
            return evidenceRoot;
        }

        public void setEvidenceRoot(String evidenceRoot) {
            this.evidenceRoot = evidenceRoot;
        }
    }

    public static class Ai {
        private boolean enabled;
        private String provider = "gemini";
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(20);
        private int retryCount = 1;
        private int maxResponseChars = 4000;
        private int maxOutputTokens = 768;
        private final Gemini gemini = new Gemini();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
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

        public int getRetryCount() {
            return retryCount;
        }

        public void setRetryCount(int retryCount) {
            this.retryCount = retryCount;
        }

        public int getMaxResponseChars() {
            return maxResponseChars;
        }

        public void setMaxResponseChars(int maxResponseChars) {
            this.maxResponseChars = maxResponseChars;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public Gemini getGemini() {
            return gemini;
        }
    }

    public static class Gemini {
        private String baseUrl = "https://generativelanguage.googleapis.com";
        private String apiKey;
        private String model = "gemini-2.5-flash";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
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
        private final RefreshCookie refreshCookie = new RefreshCookie();

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

        public RefreshCookie getRefreshCookie() {
            return refreshCookie;
        }
    }

    public static class RefreshCookie {
        private boolean enabled;
        private String name = "admin_refresh_token";
        private String path = "/api/v1/admin/auth";
        private boolean secure;
        private String sameSite = "Strict";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }

        public String getSameSite() {
            return sameSite;
        }

        public void setSameSite(String sameSite) {
            this.sameSite = sameSite;
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
