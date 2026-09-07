package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AuthResponse;
import com.omraty.backend.dto.response.UserResponse;
import com.omraty.backend.entities.User;
import com.omraty.backend.service.AuthResult;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.id(), user.phone(), user.gender(), user.identityVerified());
    }

    public static AuthResponse toAuthResponse(AuthResult result) {
        return new AuthResponse(
                result.accessToken(), result.refreshToken(), toResponse(result.user()));
    }
}
