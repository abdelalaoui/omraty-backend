package com.omraty.backend.payment.bpay;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Contre un faux serveur Bankily local (com.sun.net.httpserver), aucun appel réseau externe. */
class HttpBpayClientTest {

    private static final String TOKEN_RESPONSE =
            "{\"access_token\":\"AT\",\"expires_in\":\"300\",\"refresh_token\":\"RT\","
                    + "\"refresh_expires_in\":\"1800\"}";

    private HttpServer server;
    private final List<String> authBodies = Collections.synchronizedList(new ArrayList<>());
    private final List<String> paymentAuthHeaders = Collections.synchronizedList(new ArrayList<>());
    private Function<String, String> paymentHandler;
    private Function<String, String> checkHandler;
    private volatile long paymentDelayMs;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/authentification",
                exchange -> {
                    authBodies.add(body(exchange));
                    respond(exchange, 200, TOKEN_RESPONSE);
                });
        server.createContext(
                "/payment",
                exchange -> {
                    paymentAuthHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));
                    String requestBody = body(exchange);
                    sleep(paymentDelayMs);
                    respond(exchange, 200, paymentHandler.apply(requestBody));
                });
        server.createContext(
                "/checkTransaction",
                exchange -> respond(exchange, 200, checkHandler.apply(body(exchange))));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private HttpBpayClient client(Duration paymentTimeout) {
        return new HttpBpayClient(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                Clock.systemUTC(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/",
                "raha voyages",
                "12345",
                "ebankily",
                paymentTimeout,
                Duration.ofSeconds(2));
    }

    @Test
    void pay_success_authenticatesOnceWithPasswordGrant() {
        paymentHandler =
                request -> "{\"errorCode\":\"0\",\"errorMessage\":null,\"transactionId\":\"T1\"}";
        HttpBpayClient client = client(Duration.ofSeconds(2));

        BpayPaymentResult first = client.pay("48110011", "2580", "120", "fr", "OP1");
        BpayPaymentResult second = client.pay("48110011", "2580", "120", "fr", "OP2");

        assertThat(first.outcome()).isEqualTo(BpayPaymentResult.Outcome.SUCCESS);
        assertThat(first.transactionId()).isEqualTo("T1");
        assertThat(second.outcome()).isEqualTo(BpayPaymentResult.Outcome.SUCCESS);
        assertThat(authBodies).hasSize(1);
        assertThat(authBodies.get(0))
                .contains("grant_type=password")
                .contains("username=raha+voyages")
                .contains("client_id=ebankily");
        assertThat(paymentAuthHeaders).containsOnly("Bearer AT");
    }

    @Test
    void pay_sendsExpectedJsonBody() {
        List<String> bodies = new ArrayList<>();
        paymentHandler =
                request -> {
                    bodies.add(request);
                    return "{\"errorCode\":\"0\",\"transactionId\":\"T1\"}";
                };

        client(Duration.ofSeconds(2)).pay("48110011", "2580", "120", "fr", "OP1");

        assertThat(bodies.get(0))
                .contains("\"clientPhone\":\"48110011\"")
                .contains("\"passcode\":\"2580\"")
                .contains("\"amount\":\"120\"")
                .contains("\"language\":\"fr\"")
                .contains("\"operationId\":\"OP1\"");
    }

    @Test
    void pay_refused_returnsBankilyErrorCodeAndMessage() {
        paymentHandler = request -> "{\"errorCode\":\"1\",\"errorMessage\":\"Passcode invalide\"}";

        BpayPaymentResult result =
                client(Duration.ofSeconds(2)).pay("48110011", "9999", "120", "fr", "OP1");

        assertThat(result.outcome()).isEqualTo(BpayPaymentResult.Outcome.ERROR);
        assertThat(result.errorCode()).isEqualTo("1");
        assertThat(result.errorMessage()).isEqualTo("Passcode invalide");
    }

    @Test
    void pay_invalidToken_reauthenticatesAndRetriesOnce() {
        AtomicInteger calls = new AtomicInteger();
        paymentHandler =
                request ->
                        calls.incrementAndGet() == 1
                                ? "{\"errorCode\":\"2\",\"errorMessage\":\"invalid token\"}"
                                : "{\"errorCode\":\"0\",\"transactionId\":\"T2\"}";

        BpayPaymentResult result =
                client(Duration.ofSeconds(2)).pay("48110011", "2580", "120", "fr", "OP1");

        assertThat(result.outcome()).isEqualTo(BpayPaymentResult.Outcome.SUCCESS);
        assertThat(calls.get()).isEqualTo(2);
        // Le token encore « valide » côté client est invalidé : nouvelle authentification (le
        // refresh token est encore valide, donc grant refresh_token).
        assertThat(authBodies).hasSize(2);
        assertThat(authBodies.get(1))
                .contains("grant_type=refresh_token")
                .contains("refresh_token=RT");
    }

    @Test
    void pay_slowResponse_returnsTimeout() {
        paymentDelayMs = 1500;
        paymentHandler = request -> "{\"errorCode\":\"0\",\"transactionId\":\"T1\"}";

        BpayPaymentResult result =
                client(Duration.ofMillis(300)).pay("48110011", "2580", "120", "fr", "OP1");

        assertThat(result.outcome()).isEqualTo(BpayPaymentResult.Outcome.TIMEOUT);
    }

    @Test
    void pay_unreadableResponse_returnsTimeout() {
        paymentHandler = request -> "<html>502 Bad Gateway</html>";

        BpayPaymentResult result =
                client(Duration.ofSeconds(2)).pay("48110011", "2580", "120", "fr", "OP1");

        assertThat(result.outcome()).isEqualTo(BpayPaymentResult.Outcome.TIMEOUT);
    }

    @Test
    void checkTransaction_mapsStatuses() {
        HttpBpayClient client = client(Duration.ofSeconds(2));

        checkHandler =
                request -> "{\"errorCode\":\"0\",\"status\":\"TS\",\"transactionId\":\"T9\"}";
        BpayCheckResult success = client.checkTransaction("OP1");
        checkHandler = request -> "{\"errorCode\":\"0\",\"status\":\"TF\"}";
        BpayCheckResult failed = client.checkTransaction("OP1");
        checkHandler = request -> "{\"errorCode\":\"0\",\"status\":\"TA\"}";
        BpayCheckResult ambiguous = client.checkTransaction("OP1");
        checkHandler = request -> "{\"errorCode\":\"1\",\"errorMessage\":\"introuvable\"}";
        BpayCheckResult error = client.checkTransaction("OP1");

        assertThat(success.outcome()).isEqualTo(BpayCheckResult.Outcome.SUCCESS);
        assertThat(success.transactionId()).isEqualTo("T9");
        assertThat(failed.outcome()).isEqualTo(BpayCheckResult.Outcome.FAILED);
        assertThat(ambiguous.outcome()).isEqualTo(BpayCheckResult.Outcome.AMBIGUOUS);
        assertThat(error.outcome()).isEqualTo(BpayCheckResult.Outcome.ERROR);
        assertThat(error.errorMessage()).isEqualTo("introuvable");
    }

    private static String body(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (IOException ignored) {
            // Client parti (test de délai dépassé).
        } finally {
            exchange.close();
        }
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
