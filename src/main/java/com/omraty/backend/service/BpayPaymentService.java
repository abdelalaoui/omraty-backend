package com.omraty.backend.service;

import com.omraty.backend.dto.request.BpayPaymentRequest;
import com.omraty.backend.dto.response.BpayPaymentResponse;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.BpayTransaction;
import com.omraty.backend.entities.enums.BpayTransactionStatus;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.BookingPaymentException;
import com.omraty.backend.exception.BpayException;
import com.omraty.backend.mapper.PaymentMethodMapper;
import com.omraty.backend.payment.bpay.BpayCheckResult;
import com.omraty.backend.payment.bpay.BpayClient;
import com.omraty.backend.payment.bpay.BpayPaymentResult;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BpayTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Paiement B-PAY (API Bankily) : le client saisit dans l'app son numéro Bankily et le passcode
 * généré dans son application Bankily, le serveur appelle /payment avec un operationId unique par
 * tentative, et confirme l'achat dès le succès — sans vérification admin, contrairement aux autres
 * moyens de paiement manuels (voir PaymentMethod).
 *
 * <p>Seul le moyen de paiement payment.bpay.method-code (BANKILY par défaut) passe par ici : il
 * remplace le transfert manuel + « J'ai payé » pour Bankily. Utilisable pour le paiement dû à la
 * création (total en FULL, 1ère tranche en INSTALLMENTS) comme pour les tranches 2/3.
 *
 * <p>Cas d'exception exigés par Bankily : un refus (passcode, téléphone, montant invalide) libère
 * le paiement pour une nouvelle tentative ; un délai dépassé déclenche /checkTransaction — TS
 * confirme, TF libère, TA ou nouveau délai dépassé laissent la tentative PENDING, reprise par
 * {@link #checkPending} (voir BpayPendingCheckTask) jusqu'à un résultat définitif.
 */
@Service
public class BpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(BpayPaymentService.class);

    static final String RESULT_SUCCESS = "SUCCESS";
    static final String RESULT_FAILED = "FAILED";
    static final String RESULT_PENDING = "PENDING";

    private static final DateTimeFormatter OPERATION_ID_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyMMddHHmmss");
    private static final int MAX_OPERATION_ID_ATTEMPTS = 3;

    private final BookingPaymentService bookingPaymentService;
    private final BookingInstallmentRepository bookingInstallmentRepository;
    private final BpayTransactionRepository bpayTransactionRepository;
    private final BpayClient bpayClient;
    private final Clock clock;
    private final String methodCode;
    private final int maxChecks;
    private final int checkMinAgeSeconds;
    private final int unknownOperationFailAfterMinutes;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public BpayPaymentService(
            BookingPaymentService bookingPaymentService,
            BookingInstallmentRepository bookingInstallmentRepository,
            BpayTransactionRepository bpayTransactionRepository,
            BpayClient bpayClient,
            @Value("${payment.bpay.method-code:BANKILY}") String methodCode,
            @Value("${payment.bpay.max-checks:60}") int maxChecks,
            @Value("${payment.bpay.check-min-age-seconds:60}") int checkMinAgeSeconds,
            @Value("${payment.bpay.unknown-operation-fail-after-minutes:10}")
                    int unknownOperationFailAfterMinutes) {
        this(
                bookingPaymentService,
                bookingInstallmentRepository,
                bpayTransactionRepository,
                bpayClient,
                Clock.systemDefaultZone(),
                methodCode,
                maxChecks,
                checkMinAgeSeconds,
                unknownOperationFailAfterMinutes);
    }

    BpayPaymentService(
            BookingPaymentService bookingPaymentService,
            BookingInstallmentRepository bookingInstallmentRepository,
            BpayTransactionRepository bpayTransactionRepository,
            BpayClient bpayClient,
            Clock clock,
            String methodCode,
            int maxChecks,
            int checkMinAgeSeconds,
            int unknownOperationFailAfterMinutes) {
        this.bookingPaymentService = bookingPaymentService;
        this.bookingInstallmentRepository = bookingInstallmentRepository;
        this.bpayTransactionRepository = bpayTransactionRepository;
        this.bpayClient = bpayClient;
        this.clock = clock;
        this.methodCode = methodCode;
        this.maxChecks = maxChecks;
        this.checkMinAgeSeconds = checkMinAgeSeconds;
        this.unknownOperationFailAfterMinutes = unknownOperationFailAfterMinutes;
    }

    /** Vrai si ce moyen de paiement passe par B-PAY (voir PaymentMethodResponse.integration). */
    public boolean isBpayMethod(String paymentMethodCode) {
        return paymentMethodCode != null && paymentMethodCode.equalsIgnoreCase(methodCode);
    }

    /**
     * POST /users/me/payments/{id}/bpay (installmentId null) ou
     * /users/me/payments/{id}/installments/{installmentId}/bpay.
     *
     * <p>Volontairement non transactionnel : le verrou (voir BookingPaymentService#lockForBpay) et
     * la tentative doivent être enregistrés avant l'appel à Bankily, pour que le job de
     * vérification la retrouve même si le serveur s'arrête pendant l'appel.
     *
     * @throws BookingPaymentException.PaymentNotFoundException paiement/tranche introuvable ou
     *     appartenant à un autre utilisateur.
     * @throws BpayException.NotBpayPaymentException moyen de paiement autre que B-PAY.
     * @throws BpayException.PaymentNotPayableException rien à payer (déjà payé, expiré, tentative
     *     déjà en cours).
     */
    public BpayPaymentResponse pay(
            UUID userId,
            long paymentId,
            Long installmentId,
            BpayPaymentRequest request,
            String acceptLanguage) {
        String language = PaymentMethodMapper.resolveLanguage(acceptLanguage);
        BookingPayment payment = bookingPaymentService.getOwnedPaymentOrThrow(userId, paymentId);
        BigDecimal amount =
                installmentId == null
                        ? lockPayment(payment)
                        : lockInstallment(payment, installmentId);
        String clientPhone = normalizePhone(request.clientPhone());
        String passcode = request.passcode().trim();

        BpayTransaction transaction;
        try {
            transaction = insertTransaction(paymentId, installmentId, clientPhone, amount);
        } catch (RuntimeException e) {
            releaseLock(paymentId, installmentId, false);
            throw e;
        }

        BpayPaymentResult result;
        try {
            result =
                    bpayClient.pay(
                            clientPhone,
                            passcode,
                            formatAmount(amount),
                            language,
                            transaction.operationId());
        } catch (RuntimeException e) {
            // Le contrat de BpayClient exclut les exceptions ; si cela arrive quand même, on ne
            // sait pas si Bankily a exécuté le paiement : même traitement qu'un délai dépassé.
            log.error("B-PAY : erreur inattendue (operationId={})", transaction.operationId(), e);
            result = BpayPaymentResult.timeout();
        }

        return switch (result.outcome()) {
            case SUCCESS -> {
                applySuccess(transaction, result.transactionId());
                yield response(
                        RESULT_SUCCESS,
                        Messages.success(language),
                        transaction,
                        result.transactionId());
            }
            case ERROR -> {
                applyFailure(transaction, result.errorCode(), result.errorMessage(), false);
                String message =
                        BpayPaymentResult.AUTH_ERROR_CODE.equals(result.errorCode())
                                ? Messages.unavailable(language)
                                : orDefault(result.errorMessage(), Messages.failed(language));
                yield response(RESULT_FAILED, message, transaction, null);
            }
            case TIMEOUT -> checkAfterTimeout(transaction, language);
        };
    }

    private BpayPaymentResponse checkAfterTimeout(BpayTransaction transaction, String language) {
        BpayCheckResult check = bpayClient.checkTransaction(transaction.operationId());
        return switch (check.outcome()) {
            case SUCCESS -> {
                applySuccess(transaction, check.transactionId());
                yield response(
                        RESULT_SUCCESS,
                        Messages.success(language),
                        transaction,
                        check.transactionId());
            }
            case FAILED -> {
                applyFailure(transaction, "TF", check.errorMessage(), false);
                yield response(RESULT_FAILED, Messages.failed(language), transaction, null);
            }
            case AMBIGUOUS, ERROR, TIMEOUT -> {
                bpayTransactionRepository.recordCheck(
                        transaction.id(),
                        check.outcome() == BpayCheckResult.Outcome.AMBIGUOUS
                                ? "TA"
                                : check.errorCode(),
                        check.errorMessage());
                yield response(
                        RESULT_PENDING,
                        Messages.pending(language),
                        transaction,
                        check.transactionId());
            }
        };
    }

    /**
     * Reprend les tentatives sans résultat (délai dépassé, TA) : appelé périodiquement par
     * BpayPendingCheckTask. Au-delà de payment.bpay.max-checks vérifications sans réponse
     * définitive, la tentative n'est plus revérifiée — le paiement reste PENDING_VERIFICATION, dans
     * la file de vérification admin.
     *
     * <p>errorCode ≠ 0 sur checkTransaction signifie le plus souvent que Bankily ne connaît pas
     * l'operationId (l'appel /payment ne lui est jamais parvenu) : après
     * payment.bpay.unknown-operation-fail-after-minutes, la tentative est considérée échouée pour
     * que le client puisse réessayer.
     *
     * @return nombre de tentatives arrivées à un résultat définitif.
     */
    public int checkPending() {
        List<BpayTransaction> pending =
                bpayTransactionRepository.findPendingToCheck(maxChecks, checkMinAgeSeconds);
        int resolved = 0;
        for (BpayTransaction transaction : pending) {
            BpayCheckResult check = bpayClient.checkTransaction(transaction.operationId());
            switch (check.outcome()) {
                case SUCCESS -> {
                    applySuccess(transaction, check.transactionId());
                    resolved++;
                }
                case FAILED -> {
                    applyFailure(transaction, "TF", check.errorMessage(), true);
                    resolved++;
                }
                case ERROR -> {
                    if (transaction
                            .createdAt()
                            .isBefore(
                                    LocalDateTime.now(clock)
                                            .minusMinutes(unknownOperationFailAfterMinutes))) {
                        applyFailure(transaction, check.errorCode(), check.errorMessage(), true);
                        resolved++;
                    } else {
                        bpayTransactionRepository.recordCheck(
                                transaction.id(), check.errorCode(), check.errorMessage());
                    }
                }
                case AMBIGUOUS, TIMEOUT -> bpayTransactionRepository.recordCheck(
                        transaction.id(),
                        check.outcome() == BpayCheckResult.Outcome.AMBIGUOUS ? "TA" : null,
                        null);
            }
        }
        if (resolved > 0) {
            log.info(
                    "{} paiement(s) B-PAY résolu(s) par la vérification (sur {} en attente)",
                    resolved,
                    pending.size());
        }
        return resolved;
    }

    /**
     * @return le montant à payer (voir BookingPaymentService#amountDueNow).
     */
    private BigDecimal lockPayment(BookingPayment payment) {
        if (!isBpayMethod(payment.paymentMethod())) {
            throw new BpayException.NotBpayPaymentException(
                    "Ce paiement n'utilise pas B-PAY (id=" + payment.id() + ")");
        }
        if (payment.status() != PaymentStatus.PENDING) {
            throw new BpayException.PaymentNotPayableException(notPayableMessage(payment.status()));
        }
        if (payment.expiresAt() != null && payment.expiresAt().isBefore(LocalDateTime.now(clock))) {
            throw new BpayException.PaymentNotPayableException(
                    "Le délai de paiement est dépassé, merci de refaire la réservation");
        }
        BigDecimal amount = bookingPaymentService.amountDueNow(payment);
        bookingPaymentService
                .lockForBpay(payment.id())
                .orElseThrow(
                        () ->
                                new BpayException.PaymentNotPayableException(
                                        "Un paiement est déjà en cours pour cet achat"));
        return amount;
    }

    private BigDecimal lockInstallment(BookingPayment payment, long installmentId) {
        BookingInstallment installment =
                bookingInstallmentRepository
                        .findById(installmentId)
                        .filter(candidate -> candidate.bookingPaymentId() == payment.id())
                        .orElseThrow(
                                () ->
                                        new BookingPaymentException.PaymentNotFoundException(
                                                "Tranche introuvable (id=" + installmentId + ")"));
        if (!isBpayMethod(installment.paymentMethod())) {
            throw new BpayException.NotBpayPaymentException(
                    "Cette tranche n'utilise pas B-PAY (id=" + installmentId + ")");
        }
        if (installment.paidAt() != null) {
            throw new BpayException.PaymentNotPayableException("Cette tranche est déjà payée");
        }
        if (payment.status() != PaymentStatus.CONFIRMED) {
            throw new BpayException.PaymentNotPayableException(
                    "Le premier paiement doit d'abord être confirmé");
        }
        bookingPaymentService
                .lockInstallmentForBpay(installmentId)
                .orElseThrow(
                        () ->
                                new BpayException.PaymentNotPayableException(
                                        "Un paiement est déjà en cours pour cette tranche"));
        return installment.amount();
    }

    private static String notPayableMessage(PaymentStatus status) {
        return switch (status) {
            case PENDING_VERIFICATION -> "Un paiement est déjà en cours de vérification";
            case CONFIRMED -> "Ce paiement est déjà confirmé";
            case EXPIRED -> "Le délai de paiement est dépassé, merci de refaire la réservation";
            default -> "Ce paiement ne peut plus être réglé";
        };
    }

    private BpayTransaction insertTransaction(
            long paymentId, Long installmentId, String clientPhone, BigDecimal amount) {
        for (int attempt = 1; ; attempt++) {
            try {
                return bpayTransactionRepository.insert(
                        paymentId, installmentId, generateOperationId(), clientPhone, amount);
            } catch (DuplicateKeyException e) {
                if (attempt >= MAX_OPERATION_ID_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    /**
     * RV + horodatage + 4 chiffres aléatoires (ex. RV2610181432059731) : unique même si la base de
     * test est réinitialisée, ce qui compte puisque Bankily refuse un operationId déjà utilisé.
     */
    String generateOperationId() {
        return "RV"
                + LocalDateTime.now(clock).format(OPERATION_ID_TIMESTAMP)
                + String.format("%04d", random.nextInt(10_000));
    }

    private void applySuccess(BpayTransaction transaction, String transactionId) {
        if (bpayTransactionRepository
                .resolveIfPending(
                        transaction.id(), BpayTransactionStatus.SUCCESS, "0", null, transactionId)
                .isEmpty()) {
            return;
        }
        if (transaction.installmentId() != null) {
            bookingPaymentService.confirmInstallmentFromBpay(transaction.installmentId());
        } else {
            bookingPaymentService.confirmFromBpay(transaction.bookingPaymentId());
        }
    }

    private void applyFailure(
            BpayTransaction transaction,
            String errorCode,
            String errorMessage,
            boolean notifyUser) {
        if (bpayTransactionRepository
                .resolveIfPending(
                        transaction.id(),
                        BpayTransactionStatus.FAILED,
                        errorCode,
                        errorMessage,
                        null)
                .isEmpty()) {
            return;
        }
        releaseLock(transaction.bookingPaymentId(), transaction.installmentId(), notifyUser);
    }

    private void releaseLock(long paymentId, Long installmentId, boolean notifyUser) {
        if (installmentId != null) {
            bookingPaymentService.releaseInstallmentBpayLock(installmentId, notifyUser);
        } else {
            bookingPaymentService.releaseBpayLock(paymentId, notifyUser);
        }
    }

    private BpayPaymentResponse response(
            String result, String message, BpayTransaction transaction, String transactionId) {
        return new BpayPaymentResponse(
                result,
                message,
                transaction.operationId(),
                transactionId,
                transaction.amount(),
                LocalDateTime.now(clock));
    }

    /**
     * Bankily attend le numéro local à 8 chiffres (ex. 48110011) : retire espaces, "+", et
     * l'indicatif 222 éventuel. Aucune autre validation ici — un numéro invalide doit être refusé
     * par Bankily lui-même (cas « téléphone invalide » de leurs tests).
     */
    static String normalizePhone(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("00222") && digits.length() == 13) {
            return digits.substring(5);
        }
        if (digits.startsWith("222") && digits.length() == 11) {
            return digits.substring(3);
        }
        return digits;
    }

    /** 120.00 -> "120", 120.50 -> "120.5". */
    static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Messages propres à l'app ; les refus Bankily gardent leur errorMessage d'origine. */
    private static final class Messages {

        private Messages() {}

        static String success(String language) {
            return switch (language) {
                case "fr" -> "Paiement effectué avec succès";
                case "en" -> "Payment successful";
                default -> "تم الدفع بنجاح";
            };
        }

        static String failed(String language) {
            return switch (language) {
                case "fr" -> "Le paiement n'a pas abouti. Vérifiez le numéro et le code puis réessayez.";
                case "en" -> "The payment did not go through. Check the number and code, then try again.";
                default -> "تعذّر إتمام الدفع. تحقّق من الرقم والرمز ثم أعد المحاولة.";
            };
        }

        static String pending(String language) {
            return switch (language) {
                case "fr" -> "Votre paiement est en cours de vérification auprès de Bankily. Vous serez"
                        + " notifié dès sa confirmation.";
                case "en" -> "Your payment is being verified with Bankily. You will be notified once it"
                        + " is confirmed.";
                default -> "عملية الدفع قيد التحقق لدى بنكيلي. سنُعلمك فور تأكيدها.";
            };
        }

        static String unavailable(String language) {
            return switch (language) {
                case "fr" -> "Le service Bankily est momentanément indisponible. Réessayez plus tard.";
                case "en" -> "Bankily is temporarily unavailable. Please try again later.";
                default -> "خدمة بنكيلي غير متاحة حالياً. حاول مرة أخرى لاحقاً.";
            };
        }
    }
}
