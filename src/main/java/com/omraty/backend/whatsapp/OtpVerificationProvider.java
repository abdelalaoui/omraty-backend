package com.omraty.backend.whatsapp;

/**
 * Abstraction de la génération/l'envoi/la vérification du code OTP de connexion (voir
 * AuthService.requestOtp/loginWithOtp) — ne couvre pas le contournement par numéro de test
 * (OTP_TEST_PHONE_NUMBERS_SETTING_KEY), géré directement par AuthService. Deux implémentations
 * selon whatsapp.otp.provider (voir application.yml) : {@link LocalOtpVerificationProvider} (mock
 * | meta — on génère/stocke/vérifie nous-mêmes le code, on délègue juste son envoi à {@link
 * WhatsAppOtpSender}) et {@link TwilioVerifyOtpProvider} (twilio — Twilio Verify génère, stocke et
 * vérifie lui-même le code).
 */
public interface OtpVerificationProvider {

    /**
     * Génère (ou déclenche la génération côté fournisseur) et envoie un nouveau code OTP par
     * WhatsApp au numéro {@code phone}. Lève AuthException.OtpRequestTooSoonException si une
     * demande trop récente existe déjà, ou AuthException.OtpSendFailedException si l'envoi échoue.
     */
    void requestCode(String phone);

    /**
     * Vérifie que {@code code} correspond bien au dernier code demandé pour {@code phone}. Lève
     * AuthException.InvalidCredentialsException si le code est invalide, expiré, déjà utilisé ou
     * qu'aucun code n'a été demandé.
     */
    void verifyCode(String phone, String code);
}
