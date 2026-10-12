package com.omraty.backend.whatsapp;

import com.omraty.backend.entities.OtpCode;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.repository.OtpCodeRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * Implémentation historique de {@link OtpVerificationProvider} : on génère nous-mêmes le code, on
 * le stocke dans otp_code (voir migration V44) et on délègue son envoi à {@link WhatsAppOtpSender}
 * (Mock ou Meta, voir whatsapp.otp.provider). Active pour tout provider autre que "twilio" (voir
 * {@link TwilioVerifyOtpProvider}, qui gère lui-même la génération/le stockage/la vérification côté
 * Twilio) — reprend exactement le comportement qu'avait AuthService avant ce découpage.
 */
@Service
@ConditionalOnExpression("'${whatsapp.otp.provider:mock}' != 'twilio'")
public class LocalOtpVerificationProvider implements OtpVerificationProvider {

    static final int OTP_CODE_EXPIRATION_MINUTES = 5;
    static final int OTP_CODE_MAX_ATTEMPTS = 5;

    // Même délai que le compte à rebours de renvoi côté app (voir OtpVerificationScreen,
    // _resendCooldownSeconds) : évite qu'un appel direct à l'API contourne ce délai.
    static final int OTP_REQUEST_COOLDOWN_SECONDS = 60;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final WhatsAppOtpSender whatsAppOtpSender;

    public LocalOtpVerificationProvider(
            OtpCodeRepository otpCodeRepository, WhatsAppOtpSender whatsAppOtpSender) {
        this.otpCodeRepository = otpCodeRepository;
        this.whatsAppOtpSender = whatsAppOtpSender;
    }

    @Override
    public void requestCode(String phone) {
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
        try {
            whatsAppOtpSender.sendOtp(phone, code);
        } catch (WhatsAppSendException e) {
            throw new AuthException.OtpSendFailedException(e.getMessage(), e);
        }
    }

    @Override
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
