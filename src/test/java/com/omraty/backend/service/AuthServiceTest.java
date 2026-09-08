package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.config.security.JwtService;
import com.omraty.backend.entities.RefreshToken;
import com.omraty.backend.entities.User;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.repository.AuthRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PHONE = "+212600000000";
    private static final String RAW_PASSWORD = "MonMotDePasse123!";
    private static final String PASSWORD_HASH = "hashed-password";
    private static final String GENDER = "MALE";
    private static final String ROLE = "USER";

    @Mock private AuthRepository authRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authRepository, jwtService, passwordEncoder);
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
                        ROLE);
    }

    private void stubTokenIssuance(String accessToken, String refreshToken) {
        when(jwtService.generateAccessToken(user.id(), user.phone(), user.role()))
                .thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user.id(), user.phone())).thenReturn(refreshToken);
        when(jwtService.getExpiration(refreshToken)).thenReturn(Instant.now().plusSeconds(3600));
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
    void login_success_returnsTokens() {
        when(authRepository.findByPhone(PHONE)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, PASSWORD_HASH)).thenReturn(true);
        stubTokenIssuance("access-token", "refresh-token");

        AuthResult result = authService.login(PHONE, RAW_PASSWORD);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user()).isEqualTo(user);
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
