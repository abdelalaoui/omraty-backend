package com.omraty.backend.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MockPaymentGatewayClientTest {

    private final MockPaymentGatewayClient client = new MockPaymentGatewayClient();

    @Test
    void createPayment_returnsCodeTransactionIdAndFutureExpiration() {
        PaymentGatewayResult result =
                client.createPayment("+22890000000", new BigDecimal("50000"), "booking-42");

        assertThat(result.paymentCode()).isNotBlank();
        assertThat(result.transactionId()).isNotBlank();
        assertThat(result.expiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void checkStatus_forUnknownTransaction_returnsFailed() {
        assertThat(client.checkStatus("unknown-transaction-id"))
                .isEqualTo(PaymentGatewayStatus.FAILED);
    }

    @Test
    void checkStatus_rightAfterCreation_isPending() {
        PaymentGatewayResult result =
                client.createPayment("+22890000000", new BigDecimal("50000"), "booking-42");

        assertThat(client.checkStatus(result.transactionId()))
                .isEqualTo(PaymentGatewayStatus.PENDING);
    }
}
