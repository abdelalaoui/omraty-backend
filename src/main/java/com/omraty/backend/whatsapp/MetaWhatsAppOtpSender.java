package com.omraty.backend.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Envoi du code OTP via l'API Meta WhatsApp Cloud (Graph API), avec un template "Authentication"
 * pré-approuvé (voir Meta Business Manager > WhatsApp Manager > Modèles de messages). Active
 * quand whatsapp.otp.provider=meta (voir application.yml) — sinon {@link MockWhatsAppOtpSender}.
 *
 * <p>Le template attendu ici n'a qu'un seul paramètre corps (le code), sans bouton "Copier le
 * code" : {@code components: [{type: "body", parameters: [{type: "text", text: code}]}]}. Si le
 * template approuvé inclut un bouton "Copier le code", ajouter un composant {@code
 * {type: "button", sub_type: "url", index: "0", parameters: [{type: "text", text: code}]}} au
 * tableau components ci-dessous.
 */
@Service
@ConditionalOnProperty(prefix = "whatsapp.otp", name = "provider", havingValue = "meta")
public class MetaWhatsAppOtpSender implements WhatsAppOtpSender {

    private static final Logger log = LoggerFactory.getLogger(MetaWhatsAppOtpSender.class);

    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ObjectMapper objectMapper;

    private final String accessToken;
    private final String phoneNumberId;
    private final String templateName;
    private final String templateLanguage;
    private final String apiVersion;

    public MetaWhatsAppOtpSender(
            ObjectMapper objectMapper,
            @Value("${whatsapp.otp.meta.access-token:}") String accessToken,
            @Value("${whatsapp.otp.meta.phone-number-id:}") String phoneNumberId,
            @Value("${whatsapp.otp.meta.template-name:otp_code}") String templateName,
            @Value("${whatsapp.otp.meta.template-language:ar}") String templateLanguage,
            @Value("${whatsapp.otp.meta.api-version:v21.0}") String apiVersion) {
        this.objectMapper = objectMapper;
        this.accessToken = accessToken;
        this.phoneNumberId = phoneNumberId;
        this.templateName = templateName;
        this.templateLanguage = templateLanguage;
        this.apiVersion = apiVersion;
        if (accessToken.isBlank() || phoneNumberId.isBlank()) {
            log.warn(
                    "whatsapp.otp.provider=meta mais WHATSAPP_ACCESS_TOKEN/WHATSAPP_PHONE_NUMBER_ID"
                            + " absents — l'envoi échouera tant qu'ils ne sont pas configurés.");
        }
    }

    @Override
    public void sendOtp(String phone, String code) {
        // L'API Meta attend le numéro au format E.164 SANS le "+" (ex. "22242661765").
        String toPhone = phone.startsWith("+") ? phone.substring(1) : phone;
        Map<String, Object> codeParameter = Map.of("type", "text", "text", code);
        Map<String, Object> bodyComponent =
                Map.of("type", "body", "parameters", List.of(codeParameter));
        Map<String, Object> template =
                Map.of(
                        "name", templateName,
                        "language", Map.of("code", templateLanguage),
                        "components", List.of(bodyComponent));
        Map<String, Object> body =
                Map.of(
                        "messaging_product", "whatsapp",
                        "to", toPhone,
                        "type", "template",
                        "template", template);
        String url =
                "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(15))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return;
            }
            String errorMessage = extractErrorMessage(response.body());
            log.error(
                    "Échec envoi OTP WhatsApp (phone={}, status={}) : {}",
                    phone,
                    response.statusCode(),
                    response.body());
            throw new WhatsAppSendException(
                    "Échec de l'envoi du code par WhatsApp : " + errorMessage);
        } catch (WhatsAppSendException e) {
            throw e;
        } catch (Exception e) {
            log.error("Échec envoi OTP WhatsApp (phone={})", phone, e);
            throw new WhatsAppSendException("Échec de l'envoi du code par WhatsApp", e);
        }
    }

    /** Message d'erreur Meta (voir doc Graph API) : {error: {message: "..."}}. */
    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode message = root.path("error").path("message");
            return message.isMissingNode() ? responseBody : message.asText();
        } catch (Exception parseError) {
            return responseBody;
        }
    }
}
