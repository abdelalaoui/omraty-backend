-- Tentatives de paiement B-PAY (API Bankily, voir BpayPaymentService) : une ligne par appel à
-- POST /payment, avec son propre operation_id (exigé unique par Bankily — une nouvelle tentative
-- après un échec en génère un nouveau). installment_id renseigné pour une tranche 2/3 d'un plan
-- INSTALLMENTS, NULL pour le paiement dû à la création (montant complet en FULL, 1ère tranche en
-- INSTALLMENTS, voir BookingPayment.paymentReference).
--
-- status : PENDING tant que le résultat n'est pas connu (appel en cours, délai dépassé, ou statut
-- TA renvoyé par checkTransaction) — repris par BpayPendingCheckService jusqu'à SUCCESS/FAILED.
-- check_count/last_checked_at : suivi des appels à checkTransaction. Le passcode n'est jamais
-- stocké.
CREATE TABLE bpay_transaction (
    id                 BIGSERIAL PRIMARY KEY,
    booking_payment_id BIGINT       NOT NULL REFERENCES booking_payment (id),
    installment_id     BIGINT REFERENCES booking_installment (id),
    operation_id       VARCHAR(50)  NOT NULL UNIQUE,
    client_phone       VARCHAR(20)  NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    error_code         VARCHAR(10),
    error_message      TEXT,
    transaction_id     VARCHAR(100),
    check_count        INT          NOT NULL DEFAULT 0,
    last_checked_at    TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT chk_bpay_transaction_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED'))
);

CREATE INDEX idx_bpay_transaction_pending ON bpay_transaction (created_at) WHERE status = 'PENDING';
CREATE INDEX idx_bpay_transaction_booking_payment ON bpay_transaction (booking_payment_id);
