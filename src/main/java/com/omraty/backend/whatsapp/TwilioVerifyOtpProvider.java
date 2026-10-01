package com.omraty.backend.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omraty.backend.exception.AuthException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

/**
 * Envoi + vérification du code OTP via Twilio Verify (https://verify.twilio.com) : contrairement au
 * couple {@link WhatsAppOtpSender}/OtpCodeRepository (voir {@link LocalOtpVerificationProvider} ),
 * c'est Twilio qui génère, stocke et vérifie lui-même le code — voir
 * https://www.twilio.com/docs/verify/api/verification. Actif quand whatsapp.otp.provider=twilio
 * (voir application.yml) — sinon {@link LocalOtpVerificationProvider}.
 */
@Service
@ConditionalOnProperty(prefix = "whatsapp.otp", name = "provider", havingValue = "twilio")
public class TwilioVerifyOtpProvider implements OtpVerificationProvider {

    private static final Logger log = LoggerFactory.getLogger(TwilioVerifyOtpProvider.class);
    private static final String BASE_URL = "https://verify.twilio.com/v2/Services/";

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;

    private final String accountSid;
    private final String authToken;
    private final String verifyServiceSid;

    public TwilioVerifyOtpProvider(
            ObjectMapper objectMapper,
            @Value("${whatsapp.otp.twilio.account-sid:}") String accountSid,
            @Value("${whatsapp.otp.twilio.auth-token:}") String authToken,
            @Value("${whatsapp.otp.twilio.verify-service-sid:}") String verifyServiceSid) {
        this.objectMapper = objectMapper;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.verifyServiceSid = verifyServiceSid;
        if (accountSid.isBlank() || authToken.isBlank() || verifyServiceSid.isBlank()) {
            log.warn(
                    "whatsapp.otp.provider=twilio mais TWILIO_ACCOUNT_SID/TWILIO_AUTH_TOKEN/"
                            + "TWILIO_VERIFY_SERVICE_SID absents — les appels échoueront tant"
                            + " qu'ils ne sont pas configurés.");
        }
    }

    @Override
    public void requestCode(String phone) {
        JsonNode response =
                call("/Verifications", "To=" + encode(phone) + "&Channel=whatsapp", phone);
        log.info(
                "Code Twilio Verify envoyé (phone={}, status={})",
                phone,
                response.path("status").asText(""));
    }

    @Override
    public void verifyCode(String phone, String code) {
        JsonNode response =
                call("/VerificationCheck", "To=" + encode(phone) + "&Code=" + encode(code), phone);
        if (!"approved".equals(response.path("status").asText(""))) {
            throw new AuthException.InvalidCredentialsException("Code invalide");
        }
    }

    private JsonNode call(String path, String formBody, String phone) {
        String url = BASE_URL + verifyServiceSid + path;
        String credentials =
                Base64.getEncoder()
                        .encodeToString(
                                (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        try {
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Basic " + credentials)
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(formBody))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode body = objectMapper.readTree(response.body());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return body;
            }
            throw translateError(path, response.statusCode(), body);
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Échec appel Twilio Verify {} (phone={})", path, phone, e);
            throw new AuthException.OtpSendFailedException(
                    "Échec de l'envoi du code par WhatsApp", e);
        }
    }

    /**
     * Traduit les codes d'erreur Twilio (voir https://www.twilio.com/docs/api/errors) en exceptions
     * métier : 60203 (trop d'envois rapprochés) et 60202 (trop de tentatives de vérification)
     * rejoignent les mêmes messages que l'ancien flux local (voir LocalOtpVerificationProvider)
     * pour que l'app n'ait rien à changer côté affichage ; 60404/20404 (aucune vérification en
     * cours, ex. code expiré côté Twilio) devient "aucun code demandé".
     */
    private AuthException translateError(String path, int statusCode, JsonNode body) {
        int twilioCode = body.path("code").asInt(-1);
        String message = body.path("message").asText("Erreur Twilio Verify");
        if ("/Verifications".equals(path) && (twilioCode == 60203 || statusCode == 429)) {
            return new AuthException.OtpRequestTooSoonException(
                    "Merci de patienter avant de redemander un code");
        }
        if (twilioCode == 60202) {
            return new AuthException.InvalidCredentialsException(
                    "Trop de tentatives, redemandez un code");
        }
        if (twilioCode == 60404 || twilioCode == 20404) {
            return new AuthException.InvalidCredentialsException(
                    "Aucun code demandé pour ce numéro, redemandez un code");
        }
        log.error(
                "Erreur Twilio Verify {} (status={}, code={}) : {}",
                path,
                statusCode,
                twilioCode,
                message);
        return new AuthException.OtpSendFailedException(
                "Échec de l'envoi du code par WhatsApp : " + message, null);
    }

    private String encode(String value) {
        return UriUtils.encode(value, StandardCharsets.UTF_8);
    }
}
