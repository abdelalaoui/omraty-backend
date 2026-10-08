package com.omraty.backend.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/auth/**",
                                                // Appelé par la banque (Moov) sans JWT : voir
                                                // MoovWebhookController. Sa sécurité ne peut pas
                                                // reposer sur le JWT du client — un secret partagé
                                                // / une signature sera ajouté une fois la doc Moov
                                                // reçue.
                                                "/webhooks/**",
                                                "/app/version-check",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html",
                                                // Images publiques servies par WebConfig
                                                // (bannières, cartes de services) : affichées
                                                // via <img> (admin panel, app mobile), qui ne
                                                // peut pas envoyer de JWT. Volontairement pas
                                                // /uploads/** : identity/ et invoices/ (photos
                                                // d'identité, factures) doivent rester privés.
                                                "/uploads/banner/**",
                                                "/uploads/service-card/**")
                                        .permitAll()
                                        // Consultation du catalogue (bannières, avantages,
                                        // services, formules, packages, hôtels, groupes de
                                        // réservation, disponibilité des lits) : accessible sans
                                        // compte (voir Apple App Review Guideline 5.1.1(v) — la
                                        // connexion ne doit être exigée que pour les actions liées
                                        // au compte : réserver/payer, consulter son profil/ses
                                        // achats). Réserver/acheter reste authentifié : seules les
                                        // routes GET ci-dessous changent.
                                        .requestMatchers(
                                                HttpMethod.GET,
                                                "/home/banners",
                                                "/home/benefits",
                                                "/home/service-cards",
                                                "/service-tiers",
                                                "/packages",
                                                "/packages/*",
                                                "/hotels",
                                                "/reservation-groups",
                                                "/rooms/*/beds")
                                        .permitAll()
                                        .requestMatchers("/admin/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.POST, "/home/service-cards/**")
                                        .hasRole("ADMIN")
                                        .requestMatchers(HttpMethod.PATCH, "/home/service-cards/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
