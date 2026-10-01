package com.omraty.backend.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.OtpCode;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.repository.OtpCodeRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalOtpVerificationProviderTest {

    private static final String PHONE = "+212600000000";

    @Mock private OtpCodeRepository otpCodeRepository;
    @Mock private WhatsAppOtpSender whatsAppOtpSender;

    private LocalOtpVerificationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalOtpVerificationProvider(otpCodeRepository, whatsAppOtpSender);
    }

    private OtpCode activeOtpCode(String code, int attempts) {
        return new OtpCode(
                1L,
                PHONE,
                code,
                LocalDateTime.now().plusMinutes(5),
                attempts,
                null,
                LocalDateTime.now());
    }

    // --- verifyCode ---

    @Test
    void verifyCode_whenNoCodeRequested_throwsException() {
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.verifyCode(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void verifyCode_whenAlreadyConsumed_throwsException() {
        OtpCode consumed =
                new OtpCode(
                        1L,
                        PHONE,
                        "123456",
                        LocalDateTime.now().plusMinutes(5),
                        0,
                        LocalDateTime.now(),
                        LocalDateTime.now());
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(consumed));

        assertThatThrownBy(() -> provider.verifyCode(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void verifyCode_whenExpired_throwsException() {
        OtpCode expired =
                new OtpCode(
                        1L,
                        PHONE,
                        "123456",
                        LocalDateTime.now().minusMinutes(1),
                        0,
                        null,
                        LocalDateTime.now().minusMinutes(6));
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> provider.verifyCode(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void verifyCode_whenMaxAttemptsExceeded_throwsExceptionWithoutCheckingCode() {
        OtpCode maxedOut =
                activeOtpCode("123456", LocalOtpVerificationProvider.OTP_CODE_MAX_ATTEMPTS);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(maxedOut));

        assertThatThrownBy(() -> provider.verifyCode(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);

        verify(otpCodeRepository, never()).markConsumed(anyLong());
    }

    @Test
    void verifyCode_whenCodeWrong_incrementsAttemptsAndThrows() {
        OtpCode active = activeOtpCode("123456", 0);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> provider.verifyCode(PHONE, "000000"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);

        verify(otpCodeRepository).incrementAttempts(active.id());
    }

    @Test
    void verifyCode_whenCodeCorrect_marksConsumed() {
        OtpCode active = activeOtpCode("123456", 2);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(active));

        provider.verifyCode(PHONE, "123456");

        verify(otpCodeRepository).markConsumed(active.id());
        verify(otpCodeRepository, never()).incrementAttempts(anyLong());
    }

    // --- requestCode ---

    @Test
    void requestCode_tooSoonAfterPreviousRequest_throwsException() {
        OtpCode recentlyRequested = activeOtpCode("123456", 0);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(recentlyRequested));

        assertThatThrownBy(() -> provider.requestCode(PHONE))
                .isInstanceOf(AuthException.OtpRequestTooSoonException.class);

        verify(otpCodeRepository, never()).insert(anyString(), anyString(), any());
        verify(whatsAppOtpSender, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void requestCode_afterCooldownElapsed_generatesAndSendsNewCode() {
        LocalDateTime pastCooldown =
                LocalDateTime.now()
                        .minusSeconds(
                                LocalOtpVerificationProvider.OTP_REQUEST_COOLDOWN_SECONDS + 5);
        OtpCode oldRequest =
                new OtpCode(
                        1L,
                        PHONE,
                        "111111",
                        LocalDateTime.now().minusMinutes(10),
                        0,
                        null,
                        pastCooldown);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(oldRequest));

        provider.requestCode(PHONE);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpCodeRepository).insert(eq(PHONE), codeCaptor.capture(), any());
        String generatedCode = codeCaptor.getValue();
        assertThat(generatedCode).matches("\\d{6}");
        verify(whatsAppOtpSender).sendOtp(PHONE, generatedCode);
    }

    @Test
    void requestCode_whenNoPriorRequest_generatesAndSendsCode() {
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.empty());

        provider.requestCode(PHONE);

        verify(otpCodeRepository).insert(eq(PHONE), anyString(), any());
        verify(whatsAppOtpSender).sendOtp(eq(PHONE), anyString());
    }

    @Test
    void requestCode_whenWhatsAppSendFails_throwsOtpSendFailedException() {
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.empty());
        doThrow(new WhatsAppSendException("panne réseau"))
                .when(whatsAppOtpSender)
                .sendOtp(anyString(), anyString());

        assertThatThrownBy(() -> provider.requestCode(PHONE))
                .isInstanceOf(AuthException.OtpSendFailedException.class);
    }
}
