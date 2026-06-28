package com.codex.flashsale.channel.ingress;

import com.codex.flashsale.common.domain.SalesChannel;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TikTokIngressReceiptRepository extends JpaRepository<TikTokIngressReceipt, String> {

    Optional<TikTokIngressReceipt> findTopByChannelOrderByProcessedAtDesc(SalesChannel channel);
}
