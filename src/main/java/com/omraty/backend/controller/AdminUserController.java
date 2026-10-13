package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AdminUserResponse;
import com.omraty.backend.mapper.UserMapper;
import com.omraty.backend.service.UserService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réservé aux comptes ROLE_ADMIN (voir SecurityConfig) : liste des comptes clients, ex.
 * ?verified=true pour compter les identités approuvées.
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> list(
            @RequestParam(value = "verified", required = false) Boolean verified) {
        return ResponseEntity.ok(
                userService.listCustomers(verified).stream()
                        .map(UserMapper::toAdminResponse)
                        .toList());
    }
}
