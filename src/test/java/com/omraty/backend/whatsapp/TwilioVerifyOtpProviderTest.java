package com.omraty.backend.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TwilioVerifyOtpProviderTest {

    @Test
    void verificationBody_withSmsAndArabic_encodesPhoneChannelAndLocale() {
        assertThat(TwilioVerifyOtpProvider.verificationBody("+22236392602", "sms", "ar"))
                .isEqualTo("To=%2B22236392602&Channel=sms&Locale=ar");
    }

    @Test
    void verificationBody_withWhatsAppAndNoLocale_omitsLocale() {
        assertThat(TwilioVerifyOtpProvider.verificationBody("+22236392602", "whatsapp", ""))
                .isEqualTo("To=%2B22236392602&Channel=whatsapp");
    }
}
