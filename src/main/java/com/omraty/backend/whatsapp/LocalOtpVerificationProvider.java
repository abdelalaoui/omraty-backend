package com.omraty.backend.whatsapp;

import com.omraty.backend.exception.AuthException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * Implémentation historique de {@link OtpVerificationProvider} : on génère nous-mêmes le code, on
 * le stocke dans otp_code (voir {@link OtpCodeStore}) et on délègue son envoi à {@link
 * WhatsAppOtpSender} (Mock ou Meta, voir whatsapp.otp.provider). Active pour tout provider autre
 * que "twilio" (voir {@link TwilioVerifyOtpProvider}, qui gère lui-même la génération/le
 * stockage/la vérification côté Twilio) — reprend exactement le comportement qu'avait AuthService
 * avant ce découpage.
 */
@Service
@ConditionalOnExpression("'${whatsapp.otp.provider:mock}' != 'twilio'")
public class LocalOtpVerificationProvider implements OtpVerificationProvider {

    private final OtpCodeStore otpCodeStore;
    private final WhatsAppOtpSender whatsAppOtpSender;

    public LocalOtpVerificationProvider(
            OtpCodeStore otpCodeStore, WhatsAppOtpSender whatsAppOtpSender) {
        this.otpCodeStore = otpCodeStore;
        this.whatsAppOtpSender = whatsAppOtpSender;
    }

    @Override
    public void requestCode(String phone) {
        String code = otpCodeStore.issueCode(phone);
        try {
            whatsAppOtpSender.sendOtp(phone, code);
        } catch (WhatsAppSendException e) {
            throw new AuthException.OtpSendFailedException(e.getMessage(), e);
        }
    }

    @Override
    public void verifyCode(String phone, String code) {
        otpCodeStore.verifyCode(phone, code);
    }
}
