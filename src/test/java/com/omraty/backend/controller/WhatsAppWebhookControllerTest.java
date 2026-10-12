package com.omraty.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** /webhooks/whatsapp est public (permitAll, voir SecurityConfig) : c'est Meta qui appelle. */
class WhatsAppWebhookControllerTest {

    private static final String TOKEN = "secret-verify-token";

    @Test
    void verify_whenTokenAndModeMatch_returnsChallenge() {
        WhatsAppWebhookController controller = new WhatsAppWebhookController(TOKEN);

        ResponseEntity<String> response = controller.verify("subscribe", TOKEN, "challenge-123");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("challenge-123");
    }

    @Test
    void verify_whenTokenDoesNotMatch_returnsForbidden() {
        WhatsAppWebhookController controller = new WhatsAppWebhookController(TOKEN);

        ResponseEntity<String> response = controller.verify("subscribe", "wrong-token", "chal");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void verify_whenModeIsNotSubscribe_returnsForbidden() {
        WhatsAppWebhookController controller = new WhatsAppWebhookController(TOKEN);

        ResponseEntity<String> response = controller.verify("unsubscribe", TOKEN, "chal");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void verify_whenConfiguredTokenIsBlank_returnsForbiddenEvenIfTokensMatch() {
        WhatsAppWebhookController controller = new WhatsAppWebhookController("");

        ResponseEntity<String> response = controller.verify("subscribe", "", "chal");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void receive_alwaysReturnsOk() {
        WhatsAppWebhookController controller = new WhatsAppWebhookController(TOKEN);

        ResponseEntity<Void> response = controller.receive("{\"entry\":[]}");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
