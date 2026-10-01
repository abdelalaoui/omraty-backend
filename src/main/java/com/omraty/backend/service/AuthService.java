package com.omraty.backend.service;

import com.omraty.backend.config.security.JwtService;
import com.omraty.backend.entities.RefreshToken;
import com.omraty.backend.entities.User;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.whatsapp.OtpVerificationProvider;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    // Package-private pour les tests (voir AuthServiceTest) — même pattern que
    // BookingPaymentService.FULL_PAYMENT_DISCOUNT_PERCENTAGE_KEY.
    static final String OTP_STATIC_CODE_SETTING_KEY = "otp_static_test_code";

    /**
     * Numéros (séparés par des virgules, voir migration V44) qui continuent d'utiliser
     * OTP_STATIC_CODE_SETTING_KEY au lieu d'un vrai code envoyé par WhatsApp — comptes de test/démo
     * (ex. review Apple, qui ne peut pas recevoir de vrai message WhatsApp).
     */
    static final String OTP_TEST_PHONE_NUMBERS_SETTING_KEY = "otp_test_phone_numbers";

    private final AuthRepository authRepository;
    private final OtpVerificationProvider otpVerificationProvider;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AppSettingService appSettingService;

    public AuthService(
            AuthRepository authRepository,
            OtpVerificationProvider otpVerificationProvider,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AppSettingService appSettingService) {
        this.authRepository = authRepository;
        this.otpVerificationProvider = otpVerificationProvider;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.appSettingService = appSettingService;
    }

    public AuthResult register(String phone, String password, String gender) {
        if (authRepository.findByPhone(phone).isPresent()) {
            throw new AuthException.PhoneAlreadyUsedException("Téléphone déjà utilisé");
        }
        String passwordHash = passwordEncoder.encode(password);
        User user = authRepository.createUser(phone, passwordHash, gender);
        return issueTokens(user);
    }

    public AuthResult login(String phone, String password) {
        User user =
                authRepository
                        .findByPhone(phone)
                        .orElseThrow(
                                () ->
                                        new AuthException.InvalidCredentialsException(
                                                "Identifiants invalides"));
        requireNotDeleted(user);
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthException.InvalidCredentialsException("Identifiants invalides");
        }
        return issueTokens(user);
    }

    /**
     * Génère et envoie par WhatsApp un nouveau code OTP (voir OtpVerificationProvider) pour un
     * numéro donné — inscription ET connexion confondues : si le numéro est inconnu, un compte est
     * créé à la volée (sans mot de passe ni genre, voir migration V46) plutôt que de rejeter la
     * demande ; AuthGate/BeneficiaryInfoScreen côté app collectent le genre (et le NNI) juste après
     * la 1ère connexion réussie. Pour les numéros listés dans OTP_TEST_PHONE_NUMBERS_SETTING_KEY
     * (voir migration V44), ne fait rien : le code est déjà connu (OTP_STATIC_CODE_SETTING_KEY),
     * aucun envoi réel n'est nécessaire.
     */
    public void requestOtp(String phone) {
        authRepository
                .findByPhone(phone)
                .orElseGet(() -> authRepository.createUserPhoneOnly(phone));
        if (isTestPhoneNumber(phone)) {
            return;
        }
        otpVerificationProvider.requestCode(phone);
    }

    /**
     * Connexion par code OTP — pas de mot de passe. Pour les numéros de test (voir
     * OTP_TEST_PHONE_NUMBERS_SETTING_KEY), le code est comparé au réglage statique
     * OTP_STATIC_CODE_SETTING_KEY ; sinon délégué à {@link OtpVerificationProvider#verifyCode}
     * (code réellement généré/envoyé par {@link #requestOtp}). Seuls les numéros déjà inscrits (via
     * /auth/register) peuvent se connecter ainsi ; un numéro inconnu doit d'abord passer par
     * l'inscription classique (le profil — genre, etc. — n'est pas collecté par ce flux minimal).
     */
    public AuthResult loginWithOtp(String phone, String code) {
        if (isTestPhoneNumber(phone)) {
            String expectedCode = appSettingService.getSetting(OTP_STATIC_CODE_SETTING_KEY).value();
            if (!expectedCode.equals(code)) {
                throw new AuthException.InvalidCredentialsException("Code invalide");
            }
        } else {
            otpVerificationProvider.verifyCode(phone, code);
        }
        User user =
                authRepository
                        .findByPhone(phone)
                        .orElseThrow(
                                () ->
                                        new AuthException.InvalidCredentialsException(
                                                "Aucun compte trouvé pour ce numéro"));
        requireNotDeleted(user);
        return issueTokens(user);
    }

    /**
     * Compte anonymisé (voir UserService.deleteAccount) : traité comme inexistant, même message
     * d'erreur que pour un numéro/mot de passe invalide afin de ne pas révéler qu'un compte a
     * existé.
     */
    private void requireNotDeleted(User user) {
        if (user.deletedAt() != null) {
            throw new AuthException.InvalidCredentialsException("Identifiants invalides");
        }
    }

    private boolean isTestPhoneNumber(String phone) {
        String raw = appSettingService.getSetting(OTP_TEST_PHONE_NUMBERS_SETTING_KEY).value();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .anyMatch(testPhone -> testPhone.equals(phone));
    }

    public AuthResult refresh(String refreshToken) {
        jwtService.validateToken(refreshToken);
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new AuthException.InvalidRefreshTokenException(
                    "Le token fourni n'est pas un refresh token");
        }

        RefreshToken storedToken =
                authRepository
                        .findRefreshToken(refreshToken)
                        .orElseThrow(
                                () ->
                                        new AuthException.InvalidRefreshTokenException(
                                                "Refresh token introuvable"));
        if (storedToken.revoked()) {
            throw new AuthException.InvalidRefreshTokenException("Refresh token révoqué");
        }
        if (storedToken.expiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException.InvalidRefreshTokenException("Refresh token expiré");
        }

        User user =
                authRepository
                        .findById(storedToken.userId())
                        .orElseThrow(
                                () ->
                                        new AuthException.InvalidRefreshTokenException(
                                                "Utilisateur introuvable"));

        authRepository.revokeRefreshToken(refreshToken);
        return issueTokens(user);
    }

    public User verifyAccessToken(String accessToken) {
        if (!jwtService.isAccessToken(accessToken)) {
            throw new AuthException.InvalidTokenException(
                    "Le token fourni n'est pas un access token", null);
        }
        UUID userId = jwtService.getUserIdFromToken(accessToken);
        return authRepository
                .findById(userId)
                .orElseThrow(
                        () ->
                                new AuthException.InvalidTokenException(
                                        "Utilisateur introuvable", null));
    }

    public void logout(String refreshToken) {
        authRepository
                .findRefreshToken(refreshToken)
                .orElseThrow(
                        () ->
                                new AuthException.InvalidRefreshTokenException(
                                        "Refresh token introuvable"));
        authRepository.revokeRefreshToken(refreshToken);
    }

    private AuthResult issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.id(), user.phone(), user.role());
        String refreshToken = jwtService.generateRefreshToken(user.id(), user.phone());
        LocalDateTime expiresAt =
                LocalDateTime.ofInstant(
                        jwtService.getExpiration(refreshToken), ZoneId.systemDefault());
        authRepository.saveRefreshToken(user.id(), refreshToken, expiresAt);
        return new AuthResult(accessToken, refreshToken, user);
    }
}
