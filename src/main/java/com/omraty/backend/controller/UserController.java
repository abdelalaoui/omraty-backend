package com.omraty.backend.controller;

import com.omraty.backend.dto.response.UserResponse;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.enums.Gender;
import com.omraty.backend.mapper.UserMapper;
import com.omraty.backend.service.UserService;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(UserMapper.toResponse(userService.getCurrentUser(userId)));
    }

    @PatchMapping(value = "/me/identity", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> updateIdentity(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("nni") String nni,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("gender") Gender gender) {
        User user = userService.updateIdentity(userId, nni, photo, gender);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal UUID userId) {
        userService.deleteAccount(userId);
        return ResponseEntity.noContent().build();
    }
}
