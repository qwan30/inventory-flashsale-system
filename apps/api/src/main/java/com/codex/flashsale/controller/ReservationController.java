package com.codex.flashsale.controller;

import com.codex.flashsale.api.ConfirmReservationResponse;
import com.codex.flashsale.api.CreateReservationRequest;
import com.codex.flashsale.api.ReleaseReservationResponse;
import com.codex.flashsale.api.ReservationResponse;
import com.codex.flashsale.application.ReservationApplicationService;
import com.codex.flashsale.common.http.HeaderNames;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class ReservationController {

    private final ReservationApplicationService reservationApplicationService;

    public ReservationController(ReservationApplicationService reservationApplicationService) {
        this.reservationApplicationService = reservationApplicationService;
    }

    @PostMapping("/flash-sales/{campaignId}/reservations")
    public ReservationResponse createReservation(
            @PathVariable String campaignId,
            @RequestHeader(HeaderNames.IDEMPOTENCY_KEY) @NotBlank String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request
    ) {
        return reservationApplicationService.reserve(campaignId, request, idempotencyKey);
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    public ConfirmReservationResponse confirmReservation(
            @PathVariable String reservationId,
            @RequestHeader(HeaderNames.IDEMPOTENCY_KEY) @NotBlank String idempotencyKey
    ) {
        return reservationApplicationService.confirm(reservationId, idempotencyKey);
    }

    @PostMapping("/reservations/{reservationId}/release")
    public ReleaseReservationResponse releaseReservation(@PathVariable String reservationId) {
        return reservationApplicationService.release(reservationId);
    }
}

