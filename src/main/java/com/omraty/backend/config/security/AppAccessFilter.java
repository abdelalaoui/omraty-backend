package com.omraty.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omraty.backend.dto.response.ErrorResponse;
import com.omraty.backend.service.AppAccessService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * App fermée (voir AppAccessService) : refuse en 423 Locked les requêtes authentifiées d'un
 * utilisateur sans accès anticipé — l'écran de fermeture de l'app ne suffit pas (ancienne version
 * de l'app, appel direct à l'API). 423 et non 401/403, que l'app interprète comme une session
 * expirée (voir ApiClient côté app). Restent toujours accessibles : authentification, état de
 * l'app, admin, webhooks, et le compte lui-même (consultation, suppression — Apple 5.1.1(v)).
 * Les requêtes anonymes ne sont pas concernées : SecurityConfig ne leur ouvre que le catalogue.
 */
@Component
public class AppAccessFilter extends OncePerRequestFilter {

    private final AppAccessService appAccessService;
    private final ObjectMapper objectMapper;

    public AppAccessFilter(AppAccessService appAccessService, ObjectMapper objectMapper) {
        this.appAccessService = appAccessService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof UUID userId)
                || isAlwaysAllowed(request)
                || isAdmin(authentication)
                || appAccessService.isAppOpen()
                || appAccessService.hasEarlyAccess(userId)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.LOCKED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                new ErrorResponse("L'application n'est pas encore ouverte"));
    }

    private static boolean isAlwaysAllowed(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
                || path.startsWith("/app/")
                || path.startsWith("/admin/")
                || path.startsWith("/webhooks/")
                || path.equals("/users/me");
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
