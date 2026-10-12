package com.omraty.backend.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implémentation active par défaut (dev/test/CI, voir whatsapp.otp.provider) : n'envoie rien, se
 * contente de logger le code pour pouvoir tester le flux sans compte WhatsApp Business configuré —
 * même rôle que MockPaymentGatewayClient pour les paiements.
 */
@Service
@ConditionalOnProperty(
        prefix = "whatsapp.otp",
        name = "provider",
        havingValue = "mock",
        matchIfMissing = true)
public class MockWhatsAppOtpSender implements WhatsAppOtpSender {

    private static final Logger log = LoggerFactory.getLogger(MockWhatsAppOtpSender.class);

    @Override
    public void sendOtp(String phone, String code) {
        log.warn(
                "[MOCK WhatsApp OTP] whatsapp.otp.provider=mock : aucun message envoyé — code pour"
                        + " {} = {}",
                phone,
                code);
    }
}
