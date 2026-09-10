package com.omraty.backend.repository;

import com.omraty.backend.entities.VipRequest;
import com.omraty.backend.entities.enums.VipRequestStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class VipRequestRepository {

    private static final RowMapper<VipRequest> VIP_REQUEST_ROW_MAPPER =
            (rs, rowNum) ->
                    new VipRequest(
                            rs.getLong("id"),
                            (UUID) rs.getObject("user_id"),
                            rs.getLong("mecca_hotel_id"),
                            rs.getObject("mecca_check_in", LocalDate.class),
                            rs.getObject("mecca_check_out", LocalDate.class),
                            rs.getLong("medina_hotel_id"),
                            rs.getObject("medina_check_in", LocalDate.class),
                            rs.getObject("medina_check_out", LocalDate.class),
                            rs.getInt("seats"),
                            rs.getString("airline"),
                            VipRequestStatus.valueOf(rs.getString("status")),
                            rs.getBigDecimal("proposed_price"),
                            rs.getObject("offer_expires_at", LocalDateTime.class),
                            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public VipRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Verrouille et renvoie la demande : à appeler en début de transaction avant approve/reject/
     * accept, pour éviter une course avec le balayage des offres expirées ou une double réponse.
     */
    public Optional<VipRequest> findByIdForUpdate(long id) {
        return jdbcTemplate
                .query(
                        VipRequestTable.SELECT_VIP_REQUEST_BY_ID_FOR_UPDATE,
                        VIP_REQUEST_ROW_MAPPER,
                        id)
                .stream()
                .findFirst();
    }

    /** Demandes en attente de traitement par un admin. */
    public List<VipRequest> findPending() {
        return jdbcTemplate.query(
                VipRequestTable.SELECT_PENDING_VIP_REQUESTS, VIP_REQUEST_ROW_MAPPER);
    }

    /** Demandes d'un client, les plus récentes d'abord. */
    public List<VipRequest> findByUserId(UUID userId) {
        return jdbcTemplate.query(
                VipRequestTable.SELECT_VIP_REQUESTS_BY_USER, VIP_REQUEST_ROW_MAPPER, userId);
    }

    public VipRequest insert(
            UUID userId,
            long meccaHotelId,
            LocalDate meccaCheckIn,
            LocalDate meccaCheckOut,
            long medinaHotelId,
            LocalDate medinaCheckIn,
            LocalDate medinaCheckOut,
            int seats,
            String airline) {
        return jdbcTemplate
                .query(
                        VipRequestTable.INSERT_VIP_REQUEST,
                        VIP_REQUEST_ROW_MAPPER,
                        userId,
                        meccaHotelId,
                        meccaCheckIn,
                        meccaCheckOut,
                        medinaHotelId,
                        medinaCheckIn,
                        medinaCheckOut,
                        seats,
                        airline)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de la création de la demande VIP"));
    }

    public VipRequest updateApprove(
            long id, BigDecimal proposedPrice, LocalDateTime offerExpiresAt) {
        return jdbcTemplate
                .query(
                        VipRequestTable.UPDATE_APPROVE,
                        VIP_REQUEST_ROW_MAPPER,
                        proposedPrice,
                        offerExpiresAt,
                        id)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Demande VIP introuvable (id=" + id + ")"));
    }

    public VipRequest updateReject(long id) {
        return jdbcTemplate
                .query(VipRequestTable.UPDATE_REJECT, VIP_REQUEST_ROW_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Demande VIP introuvable (id=" + id + ")"));
    }

    public VipRequest updateAccept(long id) {
        return jdbcTemplate
                .query(VipRequestTable.UPDATE_ACCEPT, VIP_REQUEST_ROW_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Demande VIP introuvable (id=" + id + ")"));
    }

    /**
     * @return le nombre d'offres passées à CANCELLED.
     */
    public int expireOffers() {
        return jdbcTemplate.update(VipRequestTable.EXPIRE_OFFERS);
    }
}
