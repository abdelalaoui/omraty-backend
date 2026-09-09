package com.omraty.backend.service;

import com.omraty.backend.entities.User;

public record AuthResult(String accessToken, String refreshToken, User user) {}
