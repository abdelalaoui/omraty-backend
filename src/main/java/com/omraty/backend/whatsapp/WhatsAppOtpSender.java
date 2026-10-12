package com.omraty.backend.whatsapp;

/**
 * Envoi du code OTP par WhatsApp. {@link MockWhatsAppOtpSender} est l'implémentation active par
 * défaut (dev/test, voir whatsapp.otp.provider) ; {@link MetaWhatsAppOtpSender} (Meta WhatsApp
 * Cloud API, {@code @ConditionalOnProperty(prefix = "whatsapp.otp", name = "provider", havingValue
 * = "meta")}) est la vraie implémentation, une fois les identifiants Meta configurés — même
 * principe que {@code PaymentGatewayClient}/payment.gateway.provider.
 */
public interface WhatsAppOtpSender {

    /**
     * Envoie {@code code} au numéro {@code phone} (format E.164, ex. "+22242661765"). Contrairement
     * à {@code PushSender.send} (notification annexe, jamais bloquante), une exception ici DOIT
     * remonter à l'appelant (voir AuthService.requestOtp) : si l'envoi échoue, l'utilisateur n'a
     * reçu aucun code et doit en être informé plutôt que de rester bloqué sur l'écran de
     * vérification sans savoir pourquoi.
     */
    void sendOtp(String phone, String code);
}
