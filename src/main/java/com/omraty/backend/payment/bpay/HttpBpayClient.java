package com.omraty.backend.payment.bpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Vraie implémentation de {@link BpayClient}, active quand payment.bpay.provider=live (voir
 * application.yml). base-url pointe vers l'environnement de test Bankily
 * (https://ebankily-tst.appspot.com) tant que la mise en production n'est pas validée par Bankily —
 * seules l'URL et les identifiants changent ensuite.
 *
 * <p>Token : obtenu par grant password, gardé en mémoire et renouvelé par refresh_token avant son
 * expiration (marge de {@link #EXPIRY_MARGIN}). Un errorCode 2 (token invalide) ou un HTTP 401 sur
 * /payment ou /checkTransaction force un nouveau token et rejoue l'appel une seule fois : sans
 * token valide, Bankily n'a rien exécuté, rejouer le même operationId est donc sans risque.
 */
@Service
@ConditionalOnProperty(prefix = "payment.bpay", name = "provider", havingValue = "live")
public class HttpBpayClient implements BpayClient {

    private static final Logger log = LoggerFactory.getLogger(HttpBpayClient.class);

    static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);
    private static final String INVALID_TOKEN_ERROR_CODE = "2";

    /**
     * Marqueur renvoyé par {@link #postJson} sur un HTTP 401 (token refusé) — instance propre,
     * comparée par identité, jamais renvoyée par le parseur JSON.
     */
    private static final JsonNode INVALID_TOKEN = JsonNodeFactory.instance.objectNode();

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String baseUrl;
    private final String username;
    private final String password;
    private final String clientId;
    private final Duration paymentTimeout;
    private final Duration checkTimeout;

    private String accessToken;
    private Instant accessTokenExpiresAt = Instant.EPOCH;
    private String refreshToken;
    private Instant refreshTokenExpiresAt = Instant.EPOCH;

    @Autowired
    public HttpBpayClient(
            ObjectMapper objectMapper,
            @Value("${payment.bpay.base-url}") String baseUrl,
            @Value("${payment.bpay.username:}") String username,
            @Value("${payment.bpay.password:}") String password,
            @Value("${payment.bpay.client-id:ebankily}") String clientId,
            @Value("${payment.bpay.payment-timeout-ms:30000}") long paymentTimeoutMs,
            @Value("${payment.bpay.check-timeout-ms:15000}") long checkTimeoutMs) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                objectMapper,
                Clock.systemUTC(),
                baseUrl,
                username,
                password,
                clientId,
                Duration.ofMillis(paymentTimeoutMs),
                Duration.ofMillis(checkTimeoutMs));
    }

    HttpBpayClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Clock clock,
            String baseUrl,
            String username,
            String password,
            String clientId,
            Duration paymentTimeout,
            Duration checkTimeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.paymentTimeout = paymentTimeout;
        this.checkTimeout = checkTimeout;
        if (username.isBlank() || password.isBlank()) {
            log.warn(
                    "payment.bpay.provider=live mais BPAY_USERNAME/BPAY_PASSWORD absents — les"
                            + " paiements B-PAY échoueront tant qu'ils ne sont pas configurés.");
        }
    }

    @Override
    public BpayPaymentResult pay(
            String clientPhone,
            String passcode,
            String amount,
            String language,
            String operationId) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("clientPhone", clientPhone);
        body.put("passcode", passcode);
        body.put("amount", amount);
        body.put("language", language);
        body.put("operationId", operationId);
        for (int attempt = 0; attempt < 2; attempt++) {
            String token;
            try {
                token = accessToken();
            } catch (BpayAuthException e) {
                // Aucun appel /payment n'a été fait : le paiement n'a certainement pas eu lieu.
                log.error("B-PAY : authentification impossible (operationId={})", operationId, e);
                return BpayPaymentResult.error(BpayPaymentResult.AUTH_ERROR_CODE, null);
            }
            JsonNode response = postJson("/payment", body, token, paymentTimeout, operationId);
            if (response == null) {
                return BpayPaymentResult.timeout();
            }
            if (response == INVALID_TOKEN && attempt == 0) {
                invalidateAccessToken();
                continue;
            }
            String errorCode = text(response, "errorCode");
            if (errorCode == null) {
                log.error("B-PAY /payment : réponse sans errorCode (operationId={})", operationId);
                return BpayPaymentResult.timeout();
            }
            if ("0".equals(errorCode)) {
                return BpayPaymentResult.success(text(response, "transactionId"));
            }
            if (INVALID_TOKEN_ERROR_CODE.equals(errorCode) && attempt == 0) {
                invalidateAccessToken();
                continue;
            }
            return BpayPaymentResult.error(errorCode, text(response, "errorMessage"));
        }
        return BpayPaymentResult.error(BpayPaymentResult.AUTH_ERROR_CODE, null);
    }

    @Override
    public BpayCheckResult checkTransaction(String operationId) {
        Map<String, String> body = Map.of("operationId", operationId);
        for (int attempt = 0; attempt < 2; attempt++) {
            String token;
            try {
                token = accessToken();
            } catch (BpayAuthException e) {
                log.error(
                        "B-PAY : authentification impossible pour checkTransaction (operationId={})",
                        operationId,
                        e);
                return BpayCheckResult.timeout();
            }
            JsonNode response =
                    postJson("/checkTransaction", body, token, checkTimeout, operationId);
            if (response == null) {
                return BpayCheckResult.timeout();
            }
            String errorCode = response == INVALID_TOKEN ? null : text(response, "errorCode");
            if ((response == INVALID_TOKEN || INVALID_TOKEN_ERROR_CODE.equals(errorCode))
                    && attempt == 0) {
                invalidateAccessToken();
                continue;
            }
            if (errorCode == null) {
                return BpayCheckResult.timeout();
            }
            if (!"0".equals(errorCode)) {
                return BpayCheckResult.error(errorCode, text(response, "errorMessage"));
            }
            String transactionId = text(response, "transactionId");
            String status = text(response, "status");
            if ("TS".equalsIgnoreCase(status)) {
                return BpayCheckResult.of(BpayCheckResult.Outcome.SUCCESS, transactionId);
            }
            if ("TF".equalsIgnoreCase(status)) {
                return BpayCheckResult.of(BpayCheckResult.Outcome.FAILED, transactionId);
            }
            // TA, ou statut inconnu : on ne conclut ni succès ni échec.
            return BpayCheckResult.of(BpayCheckResult.Outcome.AMBIGUOUS, transactionId);
        }
        return BpayCheckResult.timeout();
    }

    /**
     * @return le corps JSON, {@link #INVALID_TOKEN} sur un HTTP 401, ou null si le résultat est
     *     inconnu (délai dépassé, erreur réseau, corps illisible).
     */
    private JsonNode postJson(
            String path,
            Map<String, String> body,
            String token,
            Duration timeout,
            String operationId) {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(baseUrl + path))
                            .timeout(timeout)
                            .header("Authorization", "Bearer " + token)
                            .header("Content-Type", "application/json")
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            objectMapper.writeValueAsString(body)))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                return INVALID_TOKEN;
            }
            log.info(
                    "B-PAY {} (operationId={}) : HTTP {} {}",
                    path,
                    operationId,
                    response.statusCode(),
                    response.body());
            return objectMapper.readTree(response.body());
        } catch (HttpTimeoutException e) {
            log.warn("B-PAY {} : délai dépassé (operationId={})", path, operationId);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException | RuntimeException e) {
            log.error("B-PAY {} : échec de l'appel (operationId={})", path, operationId, e);
            return null;
        }
    }

    private synchronized String accessToken() {
        Instant now = clock.instant();
        if (accessToken != null && now.isBefore(accessTokenExpiresAt.minus(EXPIRY_MARGIN))) {
            return accessToken;
        }
        if (refreshToken != null && now.isBefore(refreshTokenExpiresAt.minus(EXPIRY_MARGIN))) {
            try {
                return requestToken(
                        Map.of(
                                "grant_type", "refresh_token",
                                "refresh_token", refreshToken,
                                "client_id", clientId));
            } catch (BpayAuthException e) {
                log.warn("B-PAY : échec du refresh token, nouvelle authentification", e);
            }
        }
        return requestToken(
                Map.of(
                        "grant_type", "password",
                        "username", username,
                        "password", password,
                        "client_id", clientId));
    }

    private synchronized void invalidateAccessToken() {
        accessToken = null;
        accessTokenExpiresAt = Instant.EPOCH;
    }

    private String requestToken(Map<String, String> form) {
        String encoded =
                form.entrySet().stream()
                        .map(
                                entry ->
                                        URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                                                + "="
                                                + URLEncoder.encode(
                                                        entry.getValue(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&"));
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/authentification"))
                        .timeout(checkTimeout)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(encoded))
                        .build();
        HttpResponse<String> response;
        JsonNode json;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            json = objectMapper.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BpayAuthException("Authentification B-PAY interrompue", e);
        } catch (IOException | RuntimeException e) {
            throw new BpayAuthException("Authentification B-PAY impossible", e);
        }
        String token = text(json, "access_token");
        if (response.statusCode() != 200 || token == null) {
            throw new BpayAuthException(
                    "Authentification B-PAY refusée (HTTP " + response.statusCode() + ")", null);
        }
        Instant now = clock.instant();
        accessToken = token;
        accessTokenExpiresAt = now.plusSeconds(seconds(json, "expires_in"));
        refreshToken = text(json, "refresh_token");
        refreshTokenExpiresAt = now.plusSeconds(seconds(json, "refresh_expires_in"));
        return token;
    }

    /** expires_in est documenté comme String : accepte aussi un nombre JSON. */
    private static long seconds(JsonNode json, String field) {
        String value = text(json, field);
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.path(field);
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    static class BpayAuthException extends RuntimeException {
        BpayAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
