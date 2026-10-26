package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.FamilyMember;
import com.omraty.backend.entities.User;
import com.omraty.backend.exception.UserException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.FamilyMemberRepository;
import com.omraty.backend.storage.FileStorageService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final long PAYMENT_ID = 70L;

    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private AuthRepository authRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private NotificationService notificationService;

    private FamilyMemberService familyMemberService;

    @BeforeEach
    void setUp() {
        familyMemberService =
                new FamilyMemberService(
                        familyMemberRepository,
                        authRepository,
                        fileStorageService,
                        notificationService);
    }

    private static MultipartFile photo() {
        return new MockMultipartFile("photo", "id.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private static User owner(String nni) {
        return new User(
                USER_ID,
                "+22236000000",
                null,
                "MALE",
                nni,
                "identity/me.jpg",
                true,
                LocalDateTime.now(),
                "USER",
                null);
    }

    private static FamilyMember member(
            long id, UUID userId, String nni, boolean verified, Long pendingPaymentId) {
        return new FamilyMember(
                id, userId, nni, "identity/" + id + ".jpg", verified, null, pendingPaymentId);
    }

    @Test
    void addMembersForPayment_storesEachNewMemberPendingThatPayment() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni(anyString())).thenReturn(Optional.empty());
        when(fileStorageService.store(any(), eq("identity"))).thenReturn("identity/a.jpg");
        FamilyMember first = member(1L, USER_ID, "2222222222", false, PAYMENT_ID);
        FamilyMember second = member(2L, USER_ID, "3333333333", false, PAYMENT_ID);
        when(familyMemberRepository.insert(USER_ID, "2222222222", "identity/a.jpg", PAYMENT_ID))
                .thenReturn(first);
        when(familyMemberRepository.insert(USER_ID, "3333333333", "identity/a.jpg", PAYMENT_ID))
                .thenReturn(second);

        List<FamilyMember> result =
                familyMemberService.addMembersForPayment(
                        USER_ID,
                        PAYMENT_ID,
                        List.of("2222222222", " 3333333333 "),
                        List.of(photo(), photo()));

        assertThat(result).containsExactly(first, second);
        verify(familyMemberRepository).linkToPayment(PAYMENT_ID, List.of(1L, 2L));
    }

    @Test
    void addMembersForPayment_withMemberPendingAnotherAttempt_attachesItToThisPayment() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        FamilyMember pending = member(3L, USER_ID, "2222222222", false, 60L);
        when(familyMemberRepository.findByNni("2222222222")).thenReturn(Optional.of(pending));
        FamilyMember reattached = member(3L, USER_ID, "2222222222", false, PAYMENT_ID);
        when(familyMemberRepository.attachToPendingPayment(3L, PAYMENT_ID))
                .thenReturn(Optional.of(reattached));

        List<FamilyMember> result =
                familyMemberService.addMembersForPayment(
                        USER_ID, PAYMENT_ID, List.of("2222222222"), List.of(photo()));

        assertThat(result).containsExactly(reattached);
        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void addMembersForPayment_withActiveMemberOfThisFamily_reusesItAsIs() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        FamilyMember active = member(3L, USER_ID, "2222222222", true, null);
        when(familyMemberRepository.findByNni("2222222222")).thenReturn(Optional.of(active));

        List<FamilyMember> result =
                familyMemberService.addMembersForPayment(
                        USER_ID, PAYMENT_ID, List.of("2222222222"), List.of(photo()));

        assertThat(result).containsExactly(active);
        verify(familyMemberRepository, never()).attachToPendingPayment(3L, PAYMENT_ID);
        verify(familyMemberRepository, never()).insert(any(), any(), any(), any());
    }

    @Test
    void validateNnis_withNniOfAnotherFamily_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni("2222222222"))
                .thenReturn(Optional.of(member(5L, OTHER_USER_ID, "2222222222", true, null)));

        assertThatThrownBy(() -> familyMemberService.validateNnis(USER_ID, List.of("2222222222")))
                .isInstanceOf(UserException.NniAlreadyUsedException.class);
    }

    @Test
    void validateNnis_withNniOfAnotherAccount_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni("2222222222")).thenReturn(Optional.empty());
        when(authRepository.existsOtherActiveUserWithNni("2222222222", USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> familyMemberService.validateNnis(USER_ID, List.of("2222222222")))
                .isInstanceOf(UserException.NniAlreadyUsedException.class);
    }

    @Test
    void validateNnis_withOwnNni_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));

        assertThatThrownBy(() -> familyMemberService.validateNnis(USER_ID, List.of("1111111111")))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);
    }

    @Test
    void validateNnis_withSameNniTwice_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni("2222222222")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                familyMemberService.validateNnis(
                                        USER_ID, List.of("2222222222", "2222222222")))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);
    }

    @Test
    void validateNnis_withNniNotTenDigits_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));

        assertThatThrownBy(() -> familyMemberService.validateNnis(USER_ID, List.of("12345")))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);
    }

    @Test
    void discardForPayment_deletesPendingMembersAndTheirPhotos() {
        when(familyMemberRepository.findByPendingPayment(PAYMENT_ID))
                .thenReturn(List.of(member(9L, USER_ID, "2222222222", false, PAYMENT_ID)));

        familyMemberService.discardForPayment(PAYMENT_ID);

        verify(familyMemberRepository).deleteById(9L);
        verify(fileStorageService).delete("identity/9.jpg");
    }

    @Test
    void activateForPayment_activatesItsMembers() {
        familyMemberService.activateForPayment(PAYMENT_ID);

        verify(familyMemberRepository).activateForPayment(PAYMENT_ID);
    }

    @Test
    void reject_deletesTheMemberItsPhotoAndNotifiesTheOwner() {
        when(familyMemberRepository.findById(4L))
                .thenReturn(Optional.of(member(4L, USER_ID, "2222222222", false, null)));

        familyMemberService.reject(4L);

        verify(familyMemberRepository).deleteById(4L);
        verify(fileStorageService).delete("identity/4.jpg");
        verify(notificationService).create(eq(USER_ID), anyString(), anyString());
    }

    @Test
    void approve_whenAlreadyVerified_isRefused() {
        when(familyMemberRepository.findById(4L))
                .thenReturn(Optional.of(member(4L, USER_ID, "2222222222", true, null)));

        assertThatThrownBy(() -> familyMemberService.approve(4L))
                .isInstanceOf(UserException.IdentityNotPendingException.class);
    }

    @Test
    void findTravelerNnis_putsTheOwnerFirstForAFamilyAndOnlyTheAddedPeopleOtherwise() {
        when(familyMemberRepository.findMemberNnisByPaymentIds(List.of(70L, 71L, 72L)))
                .thenReturn(
                        Map.of(
                                70L, List.of("2222222222", "3333333333"),
                                71L, List.of("4444444444")));
        when(familyMemberRepository.findPartiesByPaymentIds(List.of(70L, 71L, 72L)))
                .thenReturn(Map.of(70L, true, 71L, false));

        Map<Long, List<String>> result =
                familyMemberService.findTravelerNnis(
                        List.of(70L, 71L, 72L),
                        Map.of(70L, "1111111111", 71L, "1111111111", 72L, "1111111111"));

        // 70 : famille (titulaire en premier) ; 71 : « أكثر من ذلك » ; 72 : achat sans personnes.
        assertThat(result)
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                                70L, List.of("1111111111", "2222222222", "3333333333"),
                                71L, List.of("4444444444")));
    }
}
