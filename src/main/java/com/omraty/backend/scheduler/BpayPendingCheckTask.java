package com.omraty.backend.scheduler;

import com.omraty.backend.service.BpayPaymentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Reprend les paiements B-PAY sans résultat (délai dépassé, statut TA), voir checkPending. */
@Component
public class BpayPendingCheckTask {

    private final BpayPaymentService bpayPaymentService;

    public BpayPendingCheckTask(BpayPaymentService bpayPaymentService) {
        this.bpayPaymentService = bpayPaymentService;
    }

    @Scheduled(fixedDelayString = "${payment.bpay.check-interval-ms:60000}")
    public void checkPendingBpayPayments() {
        bpayPaymentService.checkPending();
    }
}
