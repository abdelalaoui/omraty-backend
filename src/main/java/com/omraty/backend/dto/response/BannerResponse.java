package com.omraty.backend.dto.response;

/** Vue publique d'une bannière, pour l'écran d'accueil de l'app. */
public record BannerResponse(long id, String imageUrl, String title, String description) {}
