package com.omraty.backend.whatsapp;

import com.omraty.backend.entities.OtpCode;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.repository.OtpCodeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * Codes OTP générés et vérifiés par nous-mêmes (table otp_code, voir migration V44), sans aucun
 * envoi : utilisé par {@link LocalOtpVerificationProvider} (qui envoie ensuite le code par
 * WhatsApp) et par AuthService pour les numéros d'opérateurs que Twilio ne joint pas (voir
 * AuthService.OTP_LOCAL_PHONE_PREFIXES_SETTING_KEY, migration V60), dont le code est renvoyé
 * directement à l'app. Toujours actif, quel que soit whatsapp.otp.provider.
 */
@Component
public class OtpCodeStore {

    static final int OTP_CODE_EXPIRATION_MINUTES = 5;
    static final int OTP_CODE_MAX_ATTEMPTS = 5;

    // Même délai que le compte à rebours de renvoi côté app (voir OtpVerificationScreen,
    // _resendCooldownSeconds) : évite qu'un appel direct à l'API contourne ce délai.
    static final int OTP_REQUEST_COOLDOWN_SECONDS = 60;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;

    public OtpCodeStore(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    /**
     * Génère et enregistre un nouveau code pour {@code phone}, puis le renvoie. Lève
     * AuthException.OtpRequestTooSoonException si une demande trop récente existe déjà.
     */
    public String issueCode(String phone) {
        otpCodeRepository
                .findLatest(phone)
                .ifPresent(
                        latest -> {
                            LocalDateTime nextAllowedAt =
                                    latest.createdAt().plusSeconds(OTP_REQUEST_COOLDOWN_SECONDS);
                            if (nextAllowedAt.isAfter(LocalDateTime.now())) {
                                throw new AuthException.OtpRequestTooSoonException(
                                        "Merci de patienter avant de redemander un code");
                            }
                        });
        String code = generateCode();
        otpCodeRepository.insert(
                phone, code, LocalDateTime.now().plusMinutes(OTP_CODE_EXPIRATION_MINUTES));
        return code;
    }

    /**
     * Vérifie {@code code} contre le dernier code enregistré pour {@code phone}, et le consomme
     * s'il est bon. Lève AuthException.InvalidCredentialsException si le code est invalide, expiré,
     * déjà utilisé, après trop de tentatives, ou si aucun code n'a été demandé.
     */
    public void verifyCode(String phone, String code) {
        OtpCode latest =
                otpCodeRepository
                        .findLatest(phone)
                        .orElseThrow(
                                () ->
                                        new AuthException.InvalidCredentialsException(
                                                "Aucun code demandé pour ce numéro, redemandez un"
                                                        + " code"));
        if (latest.isConsumed()) {
            throw new AuthException.InvalidCredentialsException(
                    "Ce code a déjà été utilisé, redemandez un code");
        }
        if (latest.isExpired()) {
            throw new AuthException.InvalidCredentialsException("Code expiré, redemandez un code");
        }
        if (latest.attempts() >= OTP_CODE_MAX_ATTEMPTS) {
            throw new AuthException.InvalidCredentialsException(
                    "Trop de tentatives, redemandez un code");
        }
        if (!latest.code().equals(code)) {
            otpCodeRepository.incrementAttempts(latest.id());
            throw new AuthException.InvalidCredentialsException("Code invalide");
        }
        otpCodeRepository.markConsumed(latest.id());
    }

    private String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
