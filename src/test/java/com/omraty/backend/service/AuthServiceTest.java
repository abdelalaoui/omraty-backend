package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.config.security.JwtService;
import com.omraty.backend.entities.AppSetting;
import com.omraty.backend.entities.OtpCode;
import com.omraty.backend.entities.RefreshToken;
import com.omraty.backend.entities.User;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.OtpCodeRepository;
import com.omraty.backend.whatsapp.WhatsAppOtpSender;
import com.omraty.backend.whatsapp.WhatsAppSendException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PHONE = "+212600000000";
    private static final String TEST_PHONE = "+22242661765";
    private static final String RAW_PASSWORD = "MonMotDePasse123!";
    private static final String PASSWORD_HASH = "hashed-password";
    private static final String GENDER = "MALE";
    private static final String ROLE = "USER";

    @Mock private AuthRepository authRepository;
    @Mock private OtpCodeRepository otpCodeRepository;
    @Mock private WhatsAppOtpSender whatsAppOtpSender;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppSettingService appSettingService;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService =
                new AuthService(
                        authRepository,
                        otpCodeRepository,
                        whatsAppOtpSender,
                        jwtService,
                        passwordEncoder,
                        appSettingService);
        user =
                new User(
                        UUID.randomUUID(),
                        PHONE,
                        PASSWORD_HASH,
                        GENDER,
                        null,
                        null,
                        false,
                        LocalDateTime.now(),
                        ROLE,
                        null);
        // Non-stubbé pour la plupart des tests (mot de passe, refresh...) : lenient pour éviter les
        // faux positifs Mockito "unnecessary stubbing" sur les tests qui ne l'utilisent pas.
        lenient()
                .when(appSettingService.getSetting(AuthService.OTP_TEST_PHONE_NUMBERS_SETTING_KEY))
                .thenReturn(
                        new AppSetting(
                                AuthService.OTP_TEST_PHONE_NUMBERS_SETTING_KEY, TEST_PHONE, null));
    }

    private void stubTokenIssuance(String accessToken, String refreshToken) {
        when(jwtService.generateAccessToken(user.id(), user.phone(), user.role()))
                .thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user.id(), user.phone())).thenReturn(refreshToken);
        when(jwtService.getExpiration(refreshToken)).thenReturn(Instant.now().plusSeconds(3600));
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

    @Test
    void register_whenPhoneAlreadyUsed_throwsException() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.register(PHONE, RAW_PASSWORD, GENDER))
                .isInstanceOf(AuthException.PhoneAlreadyUsedException.class);

        verify(authRepository, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void register_success_createsUserAndReturnsTokens() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(authRepository.createUser(PHONE, PASSWORD_HASH, GENDER)).thenReturn(user);
        stubTokenIssuance("access-token", "refresh-token");

        AuthResult result = authService.register(PHONE, RAW_PASSWORD, GENDER);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user()).isEqualTo(user);
        verify(authRepository).saveRefreshToken(eq(user.id()), eq("refresh-token"), any());
    }

    @Test
    void login_withWrongPassword_throwsException() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.login(PHONE, RAW_PASSWORD))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void login_withUnknownPhone_throwsException() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(PHONE, RAW_PASSWORD))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void login_whenAccountDeleted_throwsExceptionWithoutCheckingPassword() {
        User deletedUser =
                new User(
                        user.id(),
                        PHONE,
                        PASSWORD_HASH,
                        GENDER,
                        null,
                        null,
                        false,
                        LocalDateTime.now(),
                        ROLE,
                        LocalDateTime.now());
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(deletedUser));

        assertThatThrownBy(() -> authService.login(PHONE, RAW_PASSWORD))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_success_returnsTokens() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        stubTokenIssuance("access-token", "refresh-token");

        AuthResult result = authService.login(PHONE, RAW_PASSWORD);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user()).isEqualTo(user);
    }

    // --- loginWithOtp : numéro de test (code statique, voir migration V44) ---

    @Test
    void loginWithOtp_testPhoneWithWrongCode_throwsException() {
        when(appSettingService.getSetting(AuthService.OTP_STATIC_CODE_SETTING_KEY))
                .thenReturn(
                        new AppSetting(AuthService.OTP_STATIC_CODE_SETTING_KEY, "123456", null));

        assertThatThrownBy(() -> authService.loginWithOtp(TEST_PHONE, "000000"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);

        verify(authRepository, never()).findByPhone(anyString());
        verify(otpCodeRepository, never()).findLatest(anyString());
    }

    @Test
    void loginWithOtp_testPhoneWithUnknownAccount_throwsException() {
        when(appSettingService.getSetting(AuthService.OTP_STATIC_CODE_SETTING_KEY))
                .thenReturn(
                        new AppSetting(AuthService.OTP_STATIC_CODE_SETTING_KEY, "123456", null));
        when(authRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithOtp(TEST_PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void loginWithOtp_testPhoneSuccess_returnsTokensWithoutTouchingOtpCodeTable() {
        when(appSettingService.getSetting(AuthService.OTP_STATIC_CODE_SETTING_KEY))
                .thenReturn(
                        new AppSetting(AuthService.OTP_STATIC_CODE_SETTING_KEY, "123456", null));
        User testUser =
                new User(
                        UUID.randomUUID(),
                        TEST_PHONE,
                        null,
                        GENDER,
                        null,
                        null,
                        false,
                        LocalDateTime.now(),
                        ROLE,
                        null);
        when(authRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser.id(), testUser.phone(), testUser.role()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(testUser.id(), testUser.phone()))
                .thenReturn("refresh-token");
        when(jwtService.getExpiration("refresh-token")).thenReturn(Instant.now().plusSeconds(3600));

        AuthResult result = authService.loginWithOtp(TEST_PHONE, "123456");

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(otpCodeRepository, never()).findLatest(anyString());
    }

    // --- loginWithOtp : numéro réel (code généré/envoyé par WhatsApp) ---

    @Test
    void loginWithOtp_realPhoneWithNoCodeRequested_throwsException() {
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithOtp(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void loginWithOtp_realPhoneAlreadyConsumed_throwsException() {
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

        assertThatThrownBy(() -> authService.loginWithOtp(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void loginWithOtp_realPhoneExpiredCode_throwsException() {
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

        assertThatThrownBy(() -> authService.loginWithOtp(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void loginWithOtp_realPhoneMaxAttemptsExceeded_throwsExceptionWithoutCheckingCode() {
        OtpCode maxedOut = activeOtpCode("123456", AuthService.OTP_CODE_MAX_ATTEMPTS);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(maxedOut));

        assertThatThrownBy(() -> authService.loginWithOtp(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);

        verify(otpCodeRepository, never()).markConsumed(anyLong());
    }

    @Test
    void loginWithOtp_realPhoneWrongCode_incrementsAttemptsAndThrows() {
        OtpCode active = activeOtpCode("123456", 0);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> authService.loginWithOtp(PHONE, "000000"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);

        verify(otpCodeRepository).incrementAttempts(active.id());
        verify(authRepository, never()).findByPhone(anyString());
    }

    @Test
    void loginWithOtp_realPhoneAccountDeleted_throwsException() {
        OtpCode active = activeOtpCode("123456", 0);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(active));
        User deletedUser =
                new User(
                        user.id(),
                        PHONE,
                        PASSWORD_HASH,
                        GENDER,
                        null,
                        null,
                        false,
                        LocalDateTime.now(),
                        ROLE,
                        LocalDateTime.now());
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(deletedUser));

        assertThatThrownBy(() -> authService.loginWithOtp(PHONE, "123456"))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);
    }

    @Test
    void loginWithOtp_realPhoneSuccess_marksConsumedAndReturnsTokens() {
        OtpCode active = activeOtpCode("123456", 2);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(active));
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        stubTokenIssuance("access-token", "refresh-token");

        AuthResult result = authService.loginWithOtp(PHONE, "123456");

        assertThat(result.accessToken()).isEqualTo("access-token");
        verify(otpCodeRepository).markConsumed(active.id());
        verify(otpCodeRepository, never()).incrementAttempts(anyLong());
    }

    // --- requestOtp ---

    @Test
    void requestOtp_withUnknownPhone_throwsExceptionWithoutInsertingCode() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.requestOtp(PHONE))
                .isInstanceOf(AuthException.InvalidCredentialsException.class);

        verify(otpCodeRepository, never()).insert(anyString(), anyString(), any());
        verify(whatsAppOtpSender, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void requestOtp_testPhone_doesNotGenerateOrSendCode() {
        when(authRepository.findByPhone(TEST_PHONE)).thenReturn(Optional.of(user));

        authService.requestOtp(TEST_PHONE);

        verify(otpCodeRepository, never()).findLatest(anyString());
        verify(otpCodeRepository, never()).insert(anyString(), anyString(), any());
        verify(whatsAppOtpSender, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void requestOtp_tooSoonAfterPreviousRequest_throwsException() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        OtpCode recentlyRequested = activeOtpCode("123456", 0);
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.of(recentlyRequested));

        assertThatThrownBy(() -> authService.requestOtp(PHONE))
                .isInstanceOf(AuthException.OtpRequestTooSoonException.class);

        verify(otpCodeRepository, never()).insert(anyString(), anyString(), any());
        verify(whatsAppOtpSender, never()).sendOtp(anyString(), anyString());
    }

    @Test
    void requestOtp_afterCooldownElapsed_generatesAndSendsNewCode() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        LocalDateTime pastCooldown =
                LocalDateTime.now().minusSeconds(AuthService.OTP_REQUEST_COOLDOWN_SECONDS + 5);
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

        authService.requestOtp(PHONE);

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpCodeRepository).insert(eq(PHONE), codeCaptor.capture(), any());
        String generatedCode = codeCaptor.getValue();
        assertThat(generatedCode).matches("\\d{6}");
        verify(whatsAppOtpSender).sendOtp(PHONE, generatedCode);
    }

    @Test
    void requestOtp_whenNoPriorRequest_generatesAndSendsCode() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.empty());

        authService.requestOtp(PHONE);

        verify(otpCodeRepository).insert(eq(PHONE), anyString(), any());
        verify(whatsAppOtpSender).sendOtp(eq(PHONE), anyString());
    }

    @Test
    void requestOtp_whenWhatsAppSendFails_throwsOtpSendFailedException() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(otpCodeRepository.findLatest(PHONE)).thenReturn(Optional.empty());
        doThrow(new WhatsAppSendException("panne réseau"))
                .when(whatsAppOtpSender)
                .sendOtp(anyString(), anyString());

        assertThatThrownBy(() -> authService.requestOtp(PHONE))
                .isInstanceOf(AuthException.OtpSendFailedException.class);
    }

    @Test
    void refresh_withRevokedToken_throwsException() {
        String oldToken = "revoked-refresh-token";
        RefreshToken storedToken =
                new RefreshToken(
                        1L,
                        user.id(),
                        oldToken,
                        LocalDateTime.now().plusDays(1),
                        true,
                        LocalDateTime.now());
        when(jwtService.isRefreshToken(oldToken)).thenReturn(true);
        when(authRepository.findRefreshToken(oldToken)).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refresh(oldToken))
                .isInstanceOf(AuthException.InvalidRefreshTokenException.class);

        verify(authRepository, never()).revokeRefreshToken(anyString());
    }

    @Test
    void refresh_withExpiredToken_throwsException() {
        String oldToken = "expired-refresh-token";
        RefreshToken storedToken =
                new RefreshToken(
                        1L,
                        user.id(),
                        oldToken,
                        LocalDateTime.now().minusDays(1),
                        false,
                        LocalDateTime.now());
        when(jwtService.isRefreshToken(oldToken)).thenReturn(true);
        when(authRepository.findRefreshToken(oldToken)).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refresh(oldToken))
                .isInstanceOf(AuthException.InvalidRefreshTokenException.class);

        verify(authRepository, never()).revokeRefreshToken(anyString());
    }

    @Test
    void refresh_success_revokesOldTokenAndIssuesNewRotatedToken() {
        String oldToken = "old-refresh-token";
        RefreshToken storedToken =
                new RefreshToken(
                        1L,
                        user.id(),
                        oldToken,
                        LocalDateTime.now().plusDays(1),
                        false,
                        LocalDateTime.now());
        when(jwtService.isRefreshToken(oldToken)).thenReturn(true);
        when(authRepository.findRefreshToken(oldToken)).thenReturn(Optional.of(storedToken));
        when(authRepository.findById(user.id())).thenReturn(Optional.of(user));
        stubTokenIssuance("new-access-token", "new-refresh-token");

        AuthResult result = authService.refresh(oldToken);

        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.refreshToken()).isNotEqualTo(oldToken);
        verify(authRepository, times(1)).revokeRefreshToken(oldToken);
        verify(authRepository).saveRefreshToken(eq(user.id()), eq("new-refresh-token"), any());
    }

    @Test
    void logout_success_revokesToken() {
        String token = "some-refresh-token";
        RefreshToken storedToken =
                new RefreshToken(
                        1L,
                        user.id(),
                        token,
                        LocalDateTime.now().plusDays(1),
                        false,
                        LocalDateTime.now());
        when(authRepository.findRefreshToken(token)).thenReturn(Optional.of(storedToken));

        authService.logout(token);

        verify(authRepository).revokeRefreshToken(token);
    }

    @Test
    void logout_withUnknownToken_throwsException() {
        String token = "unknown-refresh-token";
        when(authRepository.findRefreshToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout(token))
                .isInstanceOf(AuthException.InvalidRefreshTokenException.class);

        verify(authRepository, never()).revokeRefreshToken(anyString());
    }
}
