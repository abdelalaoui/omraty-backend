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
}
