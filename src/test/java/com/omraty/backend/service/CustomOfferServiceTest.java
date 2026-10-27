package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.BookingPayment;
import com.omraty.backend.entities.CustomOffer;
import com.omraty.backend.entities.Notification;
import com.omraty.backend.entities.User;
import com.omraty.backend.entities.enums.CustomOfferStatus;
import com.omraty.backend.entities.enums.PaymentPlan;
import com.omraty.backend.entities.enums.PaymentStatus;
import com.omraty.backend.exception.CustomOfferException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.BookingPaymentRepository;
import com.omraty.backend.repository.CustomOfferRepository;
import com.omraty.backend.repository.PackageRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomOfferServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final BigDecimal AMOUNT = new BigDecimal("600000");

    @Mock private CustomOfferRepository customOfferRepository;
    @Mock private AuthRepository authRepository;
    @Mock private PackageRepository packageRepository;
    @Mock private BookingPaymentRepository bookingPaymentRepository;
    @Mock private BookingPaymentService bookingPaymentService;
    @Mock private NotificationService notificationService;

    private CustomOfferService customOfferService;

    @BeforeEach
    void setUp() {
        customOfferService =
                new CustomOfferService(
                        customOfferRepository,
                        authRepository,
                        packageRepository,
                        bookingPaymentRepository,
                        bookingPaymentService,
                        notificationService);
    }

    private static User client() {
        return new User(
                USER_ID,
                "+22236000000",
                null,
                "MALE",
                "1111111111",
                null,
                true,
                LocalDateTime.now(),
                "USER",
                null);
    }

    private static CustomOffer offer(
            CustomOfferStatus status, LocalDateTime expiresAt, Long paymentId) {
        return new CustomOffer(
                5L,
                USER_ID,
                "Deux chambres pour 7 personnes",
                null,
                AMOUNT,
                null,
                expiresAt,
                status,
                paymentId,
                LocalDateTime.now());
    }

    private static BookingPayment payment(long id, PaymentStatus status) {
        return new BookingPayment(
                id,
                null,
                null,
                null,
                PaymentPlan.FULL,
                status,
                AMOUNT,
                null,
                null,
                null,
                LocalDateTime.now().plusMinutes(15),
                LocalDateTime.now());
    }

    @Test
    void create_findsTheClientByLocalNumberAndSendsANotificationThatOpensThePayment() {
        when(authRepository.findByPhone("+22236000000")).thenReturn(Optional.of(client()));
        LocalDateTime deadline = LocalDateTime.now().plusDays(2);
        CustomOffer created = offer(CustomOfferStatus.PENDING, deadline, null);
        when(customOfferRepository.insert(
                        USER_ID, "Deux chambres pour 7 personnes", null, AMOUNT, null, deadline))
                .thenReturn(created);

        CustomOffer result =
                customOfferService.create(
                        "36 00 00 00",
                        " Deux chambres pour 7 personnes ",
                        "  ",
                        AMOUNT,
                        null,
                        deadline);

        assertThat(result).isEqualTo(created);
        verify(notificationService)
                .create(
                        eq(USER_ID),
                        eq("Deux chambres pour 7 personnes"),
                        anyString(),
                        eq(Notification.ACTION_PAY_CUSTOM_OFFER),
                        eq(5L));
    }

    @Test
    void create_withUnknownNumber_isRefusedWithoutNotification() {
        when(authRepository.findByPhone("+22236000000")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                customOfferService.create(
                                        "36000000",
                                        "Titre",
                                        null,
                                        AMOUNT,
                                        null,
                                        LocalDateTime.now().plusDays(1)))
                .isInstanceOf(CustomOfferException.InvalidCustomOfferRequestException.class);
        verify(notificationService, never())
                .create(any(), anyString(), anyString(), anyString(), eq(5L));
    }

    @Test
    void pay_createsAFullPaymentOfTheAmountAndAttachesIt() {
        when(customOfferRepository.findByIdForUpdate(5L))
                .thenReturn(
                        Optional.of(
                                offer(
                                        CustomOfferStatus.PENDING,
                                        LocalDateTime.now().plusDays(1),
                                        null)));
        BookingPayment created = payment(70L, PaymentStatus.PENDING);
        when(bookingPaymentService.createCustomOfferPaymentPlan(AMOUNT, null, USER_ID, "BANKILY"))
                .thenReturn(created);
        when(bookingPaymentService.attachAcceptedTerms(created, "v1", null, "ar"))
                .thenReturn(created);

        BookingPayment result = customOfferService.pay(USER_ID, 5L, "BANKILY", "v1", null, "ar");

        assertThat(result).isEqualTo(created);
        verify(customOfferRepository).attachPayment(5L, 70L);
    }

    @Test
    void pay_abandonsAPreviousAttemptStillPending() {
        when(customOfferRepository.findByIdForUpdate(5L))
                .thenReturn(
                        Optional.of(
                                offer(
                                        CustomOfferStatus.PENDING,
                                        LocalDateTime.now().plusDays(1),
                                        60L)));
        when(bookingPaymentRepository.findById(60L))
                .thenReturn(Optional.of(payment(60L, PaymentStatus.PENDING)));
        BookingPayment created = payment(70L, PaymentStatus.PENDING);
        when(bookingPaymentService.createCustomOfferPaymentPlan(AMOUNT, null, USER_ID, "MOOV"))
                .thenReturn(created);
        when(bookingPaymentService.attachAcceptedTerms(created, null, null, null))
                .thenReturn(created);

        customOfferService.pay(USER_ID, 5L, "MOOV", null, null, null);

        verify(bookingPaymentRepository).markExpiredIfPending(60L);
    }

    @Test
    void listAdminBookings_keepsOffersWhosePaymentIsLaunchedAndNotAbandoned() {
        CustomOffer paid = offer(CustomOfferStatus.PAID, LocalDateTime.now().plusDays(1), 60L);
        CustomOffer abandoned =
                new CustomOffer(
                        6L,
                        USER_ID,
                        "Abandonnée",
                        null,
                        AMOUNT,
                        null,
                        LocalDateTime.now().plusDays(1),
                        CustomOfferStatus.PENDING,
                        61L,
                        LocalDateTime.now());
        CustomOffer notStarted =
                new CustomOffer(
                        7L,
                        USER_ID,
                        "Pas lancée",
                        null,
                        AMOUNT,
                        null,
                        LocalDateTime.now().plusDays(1),
                        CustomOfferStatus.PENDING,
                        null,
                        LocalDateTime.now());
        when(customOfferRepository.findAll()).thenReturn(List.of(paid, abandoned, notStarted));
        BookingPayment confirmed = payment(60L, PaymentStatus.CONFIRMED);
        when(bookingPaymentRepository.findById(60L)).thenReturn(Optional.of(confirmed));
        when(bookingPaymentRepository.findById(61L))
                .thenReturn(Optional.of(payment(61L, PaymentStatus.EXPIRED)));
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(client()));
        UserPurchasePayment purchasePayment = mock(UserPurchasePayment.class);
        when(bookingPaymentService.toPurchasePayment(confirmed, List.of()))
                .thenReturn(purchasePayment);

        List<AdminPurchase> bookings = customOfferService.listAdminBookings();

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).id()).isEqualTo("offer-5");
        assertThat(bookings.get(0).userPhone()).isEqualTo("+22236000000");
        assertThat(bookings.get(0).customOffer()).isEqualTo(paid);
        assertThat(bookings.get(0).purchase().payment()).isSameAs(purchasePayment);
        assertThat(bookings.get(0).purchase().createdAt()).isEqualTo(confirmed.createdAt());
    }

    @Test
    void pay_whileAPaymentIsUnderVerification_isRefused() {
        when(customOfferRepository.findByIdForUpdate(5L))
                .thenReturn(
                        Optional.of(
                                offer(
                                        CustomOfferStatus.PENDING,
                                        LocalDateTime.now().plusDays(1),
                                        60L)));
        when(bookingPaymentRepository.findById(60L))
                .thenReturn(Optional.of(payment(60L, PaymentStatus.PENDING_VERIFICATION)));

        assertThatThrownBy(() -> customOfferService.pay(USER_ID, 5L, "MOOV", null, null, null))
                .isInstanceOf(CustomOfferException.CustomOfferStateException.class);
        verify(bookingPaymentService, never())
                .createCustomOfferPaymentPlan(any(), any(), any(), any());
    }

    @Test
    void pay_afterTheDeadline_isRefused() {
        when(customOfferRepository.findByIdForUpdate(5L))
                .thenReturn(
                        Optional.of(
                                offer(
                                        CustomOfferStatus.PENDING,
                                        LocalDateTime.now().minusMinutes(1),
                                        null)));

        assertThatThrownBy(() -> customOfferService.pay(USER_ID, 5L, "MOOV", null, null, null))
                .isInstanceOf(CustomOfferException.CustomOfferStateException.class);
    }

    @Test
    void pay_offerOfAnotherClient_isNotFound() {
        CustomOffer other =
                new CustomOffer(
                        5L,
                        UUID.randomUUID(),
                        "Titre",
                        null,
                        AMOUNT,
                        null,
                        LocalDateTime.now().plusDays(1),
                        CustomOfferStatus.PENDING,
                        null,
                        LocalDateTime.now());
        when(customOfferRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> customOfferService.pay(USER_ID, 5L, "MOOV", null, null, null))
                .isInstanceOf(CustomOfferException.CustomOfferNotFoundException.class);
    }

    @Test
    void cancel_paidOffer_isRefused() {
        when(customOfferRepository.findByIdForUpdate(5L))
                .thenReturn(
                        Optional.of(
                                offer(
                                        CustomOfferStatus.PAID,
                                        LocalDateTime.now().plusDays(1),
                                        60L)));

        assertThatThrownBy(() -> customOfferService.cancel(5L))
                .isInstanceOf(CustomOfferException.CustomOfferStateException.class);
    }
}
