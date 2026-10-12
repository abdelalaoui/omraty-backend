package com.omraty.backend.repository;

import com.omraty.backend.entities.PaymentMethod;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class PaymentMethodRepository {

    private static final RowMapper<PaymentMethod> PAYMENT_METHOD_ROW_MAPPER =
            (rs, rowNum) ->
                    new PaymentMethod(
                            rs.getLong("id"),
                            rs.getString("code"),
                            rs.getString("name_fr"),
                            rs.getString("name_en"),
                            rs.getString("name_ar"),
                            rs.getString("merchant_number"),
                            rs.getString("merchant_name"),
                            rs.getString("instructions_fr"),
                            rs.getString("instructions_en"),
                            rs.getString("instructions_ar"),
                            rs.getString("logo_url"),
                            rs.getBoolean("enabled"),
                            rs.getInt("display_order"),
                            rs.getObject("created_at", LocalDateTime.class),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public PaymentMethodRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Moyens de paiement actifs, triés par ordre d'affichage, pour GET /payment-methods. */
    public List<PaymentMethod> findAllEnabled() {
        return jdbcTemplate.query(
                PaymentMethodTable.SELECT_ENABLED_PAYMENT_METHODS, PAYMENT_METHOD_ROW_MAPPER);
    }

    public Optional<PaymentMethod> findByCode(String code) {
        return jdbcTemplate
                .query(
                        PaymentMethodTable.SELECT_PAYMENT_METHOD_BY_CODE,
                        PAYMENT_METHOD_ROW_MAPPER,
                        code)
                .stream()
                .findFirst();
    }

    /** Tous les moyens de paiement (actifs + inactifs), pour GET /admin/payment-methods. */
    public List<PaymentMethod> findAll() {
        return jdbcTemplate.query(
                PaymentMethodTable.SELECT_ALL_PAYMENT_METHODS, PAYMENT_METHOD_ROW_MAPPER);
    }

    public PaymentMethod insert(
            String code,
            String nameFr,
            String nameEn,
            String nameAr,
            String merchantNumber,
            String merchantName,
            String instructionsFr,
            String instructionsEn,
            String instructionsAr,
            int displayOrder,
            boolean enabled) {
        return jdbcTemplate
                .query(
                        PaymentMethodTable.INSERT_PAYMENT_METHOD,
                        PAYMENT_METHOD_ROW_MAPPER,
                        code,
                        nameFr,
                        nameEn,
                        nameAr,
                        merchantNumber,
                        merchantName,
                        instructionsFr,
                        instructionsEn,
                        instructionsAr,
                        displayOrder,
                        enabled)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Échec de la création du moyen de paiement"));
    }

    /**
     * Mise à jour partielle : seuls les champs non null sont modifiés (voir PaymentMethodTable).
     */
    public Optional<PaymentMethod> update(
            long id,
            String nameFr,
            String nameEn,
            String nameAr,
            String merchantNumber,
            String merchantName,
            String instructionsFr,
            String instructionsEn,
            String instructionsAr,
            Boolean enabled,
            Integer displayOrder) {
        return jdbcTemplate
                .query(
                        PaymentMethodTable.UPDATE_PAYMENT_METHOD,
                        PAYMENT_METHOD_ROW_MAPPER,
                        nameFr,
                        nameEn,
                        nameAr,
                        merchantNumber,
                        merchantName,
                        instructionsFr,
                        instructionsEn,
                        instructionsAr,
                        enabled,
                        displayOrder,
                        id)
                .stream()
                .findFirst();
    }

    public Optional<PaymentMethod> updateLogo(long id, String logoUrl) {
        return jdbcTemplate
                .query(
                        PaymentMethodTable.UPDATE_PAYMENT_METHOD_LOGO,
                        PAYMENT_METHOD_ROW_MAPPER,
                        logoUrl,
                        id)
                .stream()
                .findFirst();
    }
}
