package com.codex.flashsale.controller;

import com.codex.flashsale.api.TikTokIngressReceiptResponse;
import com.codex.flashsale.api.TikTokInventoryIngressRequest;
import com.codex.flashsale.api.TikTokOrderStatusIngressRequest;
import com.codex.flashsale.channel.ingress.TikTokIngressService;
import com.codex.flashsale.channel.ingress.TikTokIngressSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/channel-ingress/tiktok")
public class TikTokIngressController {

    private final ObjectMapper objectMapper;
    private final TikTokIngressSignatureVerifier signatureVerifier;
    private final TikTokIngressService tikTokIngressService;

    public TikTokIngressController(
            ObjectMapper objectMapper,
            TikTokIngressSignatureVerifier signatureVerifier,
            TikTokIngressService tikTokIngressService
    ) {
        this.objectMapper = objectMapper;
        this.signatureVerifier = signatureVerifier;
        this.tikTokIngressService = tikTokIngressService;
    }

    @PostMapping(path = "/inventory", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TikTokIngressReceiptResponse inventory(
            @RequestHeader("X-TikTok-Timestamp") String timestamp,
            @RequestHeader("X-TikTok-Signature") String signature,
            @RequestBody String rawBody
    ) throws Exception {
        signatureVerifier.verify(rawBody, timestamp, signature);
        return tikTokIngressService.ingestInventory(
                objectMapper.readValue(rawBody, TikTokInventoryIngressRequest.class),
                rawBody
        );
    }

    @PostMapping(path = "/orders/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public TikTokIngressReceiptResponse orderStatus(
            @RequestHeader("X-TikTok-Timestamp") String timestamp,
            @RequestHeader("X-TikTok-Signature") String signature,
            @RequestBody String rawBody
    ) throws Exception {
        signatureVerifier.verify(rawBody, timestamp, signature);
        return tikTokIngressService.ingestOrderStatus(
                objectMapper.readValue(rawBody, TikTokOrderStatusIngressRequest.class),
                rawBody
        );
    }
}
