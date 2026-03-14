package com.codex.flashsale.controller;

import com.codex.flashsale.api.OrderResponse;
import com.codex.flashsale.api.UpdateOrderStatusRequest;
import com.codex.flashsale.application.OrderApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping("/{orderId}/status")
    public OrderResponse updateStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderApplicationService.updateStatus(orderId, request.status());
    }
}

