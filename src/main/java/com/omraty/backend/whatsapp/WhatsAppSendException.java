package com.omraty.backend.whatsapp;

/** Échec d'appel à l'API WhatsApp (voir MetaWhatsAppOtpSender) — traduit par AuthService. */
public class WhatsAppSendException extends RuntimeException {

    public WhatsAppSendException(String message) {
        super(message);
    }

    public WhatsAppSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
