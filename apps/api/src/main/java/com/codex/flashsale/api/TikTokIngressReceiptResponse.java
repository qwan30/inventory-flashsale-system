package com.codex.flashsale.api;

public record TikTokIngressReceiptResponse(
        String receiptId,
        String outcome,
        String detail
) {
}
