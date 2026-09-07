package com.omraty.backend.service;

import com.omraty.backend.config.security.JwtService;
import com.omraty.backend.entities.RefreshToken;
import com.omraty.backend.entities.User;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.repository.AuthRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthRepository authRepository, JwtService jwtService) {
        this.authRepository = authRepository;
        this.jwtService = jwtService;
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
        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthException.InvalidCredentialsException("Identifiants invalides");
        }
        return issueTokens(user);
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
        String accessToken = jwtService.generateAccessToken(user.id(), user.phone());
        String refreshToken = jwtService.generateRefreshToken(user.id(), user.phone());
        LocalDateTime expiresAt =
                LocalDateTime.ofInstant(
                        jwtService.getExpiration(refreshToken), ZoneId.systemDefault());
        authRepository.saveRefreshToken(user.id(), refreshToken, expiresAt);
        return new AuthResult(accessToken, refreshToken, user);
    }
}
