package com.omraty.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omraty.backend.payment.WebhookVerifier;
import com.omraty.backend.service.BookingPaymentService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Tâche 10 : /webhooks/moov est public (permitAll), donc {@link WebhookVerifier} est le seul
 * garde-fou avant même de lire le contenu du webhook.
 */
@ExtendWith(MockitoExtension.class)
class MoovWebhookControllerTest {

    private static final String VALID_BODY = "{\"transactionId\":\"txn-1\",\"status\":\"SUCCESS\"}";

    @Mock private BookingPaymentService bookingPaymentService;
    @Mock private WebhookVerifier webhookVerifier;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private MoovWebhookController controller() {
        return new MoovWebhookController(
                bookingPaymentService, webhookVerifier, objectMapper, validator);
    }

    @Test
    void handleWebhook_whenVerificationFails_rejectsWithUnauthorizedAndDoesNotProcess() {
        when(webhookVerifier.verify(anyString(), anyMap())).thenReturn(false);

        ResponseEntity<Void> response = controller().handleWebhook(VALID_BODY, Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(bookingPaymentService);
    }

    @Test
    void handleWebhook_whenVerifiedAndBodyValid_confirmsPaymentAndReturnsOk() {
        when(webhookVerifier.verify(VALID_BODY, Map.of())).thenReturn(true);

        ResponseEntity<Void> response = controller().handleWebhook(VALID_BODY, Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bookingPaymentService).confirmFromGateway("txn-1", "SUCCESS");
    }

    @Test
    void handleWebhook_whenVerifiedButBodyNotValidJson_returnsBadRequestWithoutProcessing() {
        when(webhookVerifier.verify(anyString(), anyMap())).thenReturn(true);

        ResponseEntity<Void> response = controller().handleWebhook("not-json", Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(bookingPaymentService, never()).confirmFromGateway(anyString(), anyString());
    }

    @Test
    void handleWebhook_whenVerifiedButTransactionIdBlank_returnsBadRequestWithoutProcessing() {
        when(webhookVerifier.verify(anyString(), anyMap())).thenReturn(true);
        String bodyWithBlankTransactionId = "{\"transactionId\":\"\",\"status\":\"SUCCESS\"}";

        ResponseEntity<Void> response =
                controller().handleWebhook(bodyWithBlankTransactionId, Map.of());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(bookingPaymentService, never()).confirmFromGateway(anyString(), anyString());
    }
}
