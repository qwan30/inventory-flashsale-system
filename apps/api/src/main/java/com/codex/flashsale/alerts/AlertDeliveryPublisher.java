package com.codex.flashsale.alerts;

import com.codex.flashsale.api.OpsAlertResponse;
import java.time.Instant;

public interface AlertDeliveryPublisher {

    void publish(OpsAlertResponse alert, AlertDispatchType dispatchType, Instant dispatchedAt);
}
