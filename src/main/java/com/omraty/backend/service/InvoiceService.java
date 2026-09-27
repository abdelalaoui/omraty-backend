package com.omraty.backend.service;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.BookingInstallment;
import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.entities.Room;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.VipRequest;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BedRepository;
import com.omraty.backend.repository.BookingInstallmentRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.PackageRepository;
import com.omraty.backend.repository.RoomRepository;
import com.omraty.backend.repository.VipRequestRepository;
import com.omraty.backend.storage.FileStorageService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Génère et stocke la facture PDF d'un achat, une fois celui-ci intégralement payé (voir migration
 * V39 : invoice_key/invoice_number/invoice_generated_at sur booking_payment). Jamais de reçu
 * intermédiaire par tranche : plan FULL, la facture part dès la confirmation ; plan INSTALLMENTS,
 * seulement une fois les 3 tranches payées.
 *
 * <p>Best-effort : une erreur de génération/stockage est logguée mais ne remonte jamais à
 * l'appelant (voir {@link #generateIfFullyPaid}), pour ne jamais faire échouer la confirmation du
 * paiement ou le marquage d'une tranche à cause d'un problème côté facturation.
 */
@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CURRENCY = "MRU";
    private static final String INVOICE_SUBDIR = "invoices";

    private final BookingPaymentRepository bookingPaymentRepository;
    private final BookingInstallmentRepository bookingInstallmentRepository;
    private final AuthRepository authRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final VipRequestRepository vipRequestRepository;
    private final PackageRepository packageRepository;
    private final FileStorageService fileStorageService;
    private final String letterheadBase64;

    public InvoiceService(
            BookingPaymentRepository bookingPaymentRepository,
            BookingInstallmentRepository bookingInstallmentRepository,
            AuthRepository authRepository,
            RoomRepository roomRepository,
            BedRepository bedRepository,
            VipRequestRepository vipRequestRepository,
            PackageRepository packageRepository,
            FileStorageService fileStorageService) {
        this.bookingPaymentRepository = bookingPaymentRepository;
        this.bookingInstallmentRepository = bookingInstallmentRepository;
        this.authRepository = authRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.vipRequestRepository = vipRequestRepository;
        this.packageRepository = packageRepository;
        this.fileStorageService = fileStorageService;
        this.letterheadBase64 = loadLetterheadBase64();
    }

    private static String loadLetterheadBase64() {
        try (InputStream stream =
                InvoiceService.class.getResourceAsStream("/invoice/letterhead.png")) {
            if (stream == null) {
                throw new IOException("Ressource /invoice/letterhead.png introuvable");
            }
            return Base64.getEncoder().encodeToString(stream.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de charger le papier à en-tête", e);
        }
    }

    /**
     * Vérifie si {@code paymentId} vient de passer à intégralement payé et, si oui, génère et
     * stocke sa facture — jamais avant, jamais deux fois. À appeler après tout événement pouvant
     * amener un achat à ce point : confirmation du paiement (voir
     * BookingPaymentService.confirmFromGateway) et marquage manuel d'une tranche (voir
     * BookingPaymentService.markInstallmentPaid).
     */
    public void generateIfFullyPaid(long paymentId) {
        try {
            doGenerateIfFullyPaid(paymentId);
        } catch (Exception e) {
            log.error(
                    "Échec de la génération de la facture pour le paiement {} : la confirmation du"
                            + " paiement reste acquise, la facture pourra être régénérée plus"
                            + " tard.",
                    paymentId,
                    e);
        }
    }

    private void doGenerateIfFullyPaid(long paymentId) {
        BookingPayment payment = bookingPaymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.status() != PaymentStatus.CONFIRMED) {
            return;
        }
        List<BookingInstallment> installments =
                bookingInstallmentRepository.findByBookingPaymentIds(List.of(paymentId));
        if (!isFullyPaid(payment, installments)) {
            return;
        }
        if (bookingPaymentRepository.hasInvoiceGenerated(paymentId)) {
            return;
        }
        PurchaseDescription purchase = resolvePurchaseDescription(payment);
        if (purchase == null) {
            log.warn(
                    "Facture non générée pour le paiement {} : contexte d'achat introuvable"
                            + " (chambre/lit/offre VIP ou client supprimé entre-temps).",
                    paymentId);
            return;
        }
        String invoiceNumber = buildInvoiceNumber(paymentId);
        byte[] pdf = renderPdf(payment, installments, purchase, invoiceNumber);
        String invoiceKey =
                fileStorageService.store(
                        pdf, invoiceNumber + ".pdf", "application/pdf", INVOICE_SUBDIR);
        boolean generated =
                bookingPaymentRepository.markInvoiceGenerated(paymentId, invoiceKey, invoiceNumber);
        if (!generated) {
            // Perdu la course contre un autre déclenchement concurrent (webhook + marquage manuel
            // d'une tranche, ou rejeu) : la facture qu'il a stockée fait foi, celle-ci est un
            // doublon orphelin sans conséquence fonctionnelle.
            log.info(
                    "Facture déjà générée entre-temps pour le paiement {} (course concurrente),"
                            + " doublon ignoré.",
                    paymentId);
        }
    }

    private static boolean isFullyPaid(
            BookingPayment payment, List<BookingInstallment> installments) {
        if (payment.plan() == PaymentPlan.FULL) {
            return true;
        }
        return installments.size() == 3
                && installments.stream().allMatch(installment -> installment.paidAt() != null);
    }

    private static String buildInvoiceNumber(long paymentId) {
        return String.format("FACT-%d-%06d", LocalDate.now().getYear(), paymentId);
    }

    private PurchaseDescription resolvePurchaseDescription(BookingPayment payment) {
        if (payment.roomId() != null) {
            return roomRepository.findByIds(List.of(payment.roomId())).stream()
                    .findFirst()
                    .map(this::describeRoom)
                    .orElse(null);
        }
        if (payment.bedId() != null) {
            return bedRepository.findByIds(List.of(payment.bedId())).stream()
                    .findFirst()
                    .flatMap(this::describeBed)
                    .orElse(null);
        }
        return vipRequestRepository.findByIds(List.of(payment.vipRequestId())).stream()
                .findFirst()
                .map(this::describeVipRequest)
                .orElse(null);
    }

    private PurchaseDescription describeRoom(Room room) {
        String label =
                "Achat chambre "
                        + room.totalCapacity()
                        + " places — "
                        + resolvePackageLabel(room.packageId());
        return new PurchaseDescription(room.userId(), label);
    }

    private Optional<PurchaseDescription> describeBed(Bed bed) {
        return roomRepository.findByIds(List.of(bed.roomId())).stream()
                .findFirst()
                .map(
                        room ->
                                new PurchaseDescription(
                                        bed.userId(),
                                        "Réservation lit n°"
                                                + bed.number()
                                                + " (chambre "
                                                + room.totalCapacity()
                                                + " places) — "
                                                + resolvePackageLabel(room.packageId())));
    }

    private PurchaseDescription describeVipRequest(VipRequest vipRequest) {
        String label =
                "Offre VIP — vol "
                        + vipRequest.airline()
                        + ", "
                        + vipRequest.seats()
                        + " place(s) — "
                        + resolvePackageLabel(vipRequest.packageId());
        return new PurchaseDescription(vipRequest.userId(), label);
    }

    private String resolvePackageLabel(long packageId) {
        return packageRepository
                .findById(packageId)
                .map(OmraPackage::label)
                .orElse("Package #" + packageId);
    }

    private byte[] renderPdf(
            BookingPayment payment,
            List<BookingInstallment> installments,
            PurchaseDescription purchase,
            String invoiceNumber) {
        String phone = authRepository.findById(purchase.userId()).map(User::phone).orElse("—");
        String html = buildHtml(payment, installments, purchase, invoiceNumber, phone);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Échec du rendu PDF de la facture", e);
        }
    }

    private String buildHtml(
            BookingPayment payment,
            List<BookingInstallment> installments,
            PurchaseDescription purchase,
            String invoiceNumber,
            String phone) {
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
                .append(
                        "@page { size: 218mm 339mm; margin: 0; }"
                                + " body { margin: 0; font-family: Helvetica, Arial, sans-serif;"
                                + " font-size: 11pt; color: #2b2b2b;"
                                + " background-image: url('data:image/png;base64,")
                .append(letterheadBase64)
                .append(
                        "'); background-size: 100% 100%; background-repeat: no-repeat; }"
                                + " .content { padding: 55mm 15mm 40mm 15mm; }"
                                + " h1 { font-size: 16pt; margin: 0 0 4mm 0; }"
                                + " .meta { margin-bottom: 6mm; }"
                                + " table { width: 100%; border-collapse: collapse; margin-bottom:"
                                + " 6mm; }"
                                + " th, td { border: 1px solid #999; padding: 2mm 3mm; text-align:"
                                + " left; font-size: 10pt; }"
                                + " th { background-color: rgba(255,255,255,0.6); }"
                                + " .totals td { border: none; padding: 1mm 3mm; }"
                                + " .totals .label { text-align: right; font-weight: bold; }")
                .append("</style></head><body><div class=\"content\">");

        html.append("<h1>Facture ").append(escape(invoiceNumber)).append("</h1>");
        html.append("<div class=\"meta\">");
        html.append("Date : ").append(LocalDate.now().format(DATE_FORMAT)).append("<br/>");
        html.append("Client : ").append(escape(phone));
        html.append("</div>");

        html.append("<table><tr><th>Description</th><th>Montant</th></tr>");
        html.append("<tr><td>")
                .append(escape(purchase.description()))
                .append("</td><td>")
                .append(formatAmount(payment.totalAmount()))
                .append("</td></tr></table>");

        html.append("<table class=\"totals\">");
        html.append("<tr><td class=\"label\">Total</td><td>")
                .append(formatAmount(payment.totalAmount()))
                .append("</td></tr>");
        html.append("<tr><td class=\"label\">Payé</td><td>")
                .append(formatAmount(payment.totalAmount()))
                .append("</td></tr>");
        html.append("<tr><td class=\"label\">Restant</td><td>")
                .append(formatAmount(BigDecimal.ZERO))
                .append("</td></tr>");
        html.append("</table>");

        if (payment.plan() == PaymentPlan.INSTALLMENTS) {
            html.append(
                    "<table><tr><th>Tranche</th><th>Montant</th><th>Échéance</th><th>Payée"
                            + " le</th></tr>");
            for (BookingInstallment installment : installments) {
                html.append("<tr><td>")
                        .append(installment.sequence())
                        .append("</td><td>")
                        .append(formatAmount(installment.amount()))
                        .append("</td><td>")
                        .append(installment.dueDate().format(DATE_FORMAT))
                        .append("</td><td>")
                        .append(formatPaidAt(installment.paidAt()))
                        .append("</td></tr>");
            }
            html.append("</table>");
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP) + " " + CURRENCY;
    }

    private static String formatPaidAt(LocalDateTime paidAt) {
        return paidAt == null ? "—" : paidAt.toLocalDate().format(DATE_FORMAT);
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record PurchaseDescription(UUID userId, String description) {}
}
