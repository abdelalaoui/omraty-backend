package com.omraty.backend.scheduler;

import com.omraty.backend.service.CustomOfferService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Propositions dont la date limite est dépassée sans paiement : passées à EXPIRED chaque minute
 * (voir CustomOfferService.expireOverdue). Même pattern que PaymentExpirationTask.
 */
@Component
public class CustomOfferExpirationTask {

    private final CustomOfferService customOfferService;

    public CustomOfferExpirationTask(CustomOfferService customOfferService) {
        this.customOfferService = customOfferService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void expireOverdueOffers() {
        customOfferService.expireOverdue();
    }
}
