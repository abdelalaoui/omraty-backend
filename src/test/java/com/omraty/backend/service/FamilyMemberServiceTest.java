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

    private static FamilyMember member(long id, UUID userId, String nni, boolean verified) {
        return new FamilyMember(id, userId, nni, "identity/" + id + ".jpg", verified, null);
    }

    @Test
    void addMembers_storesEachNewMemberWithItsPhoto() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni(anyString())).thenReturn(Optional.empty());
        when(fileStorageService.store(any(), eq("identity"))).thenReturn("identity/a.jpg");
        FamilyMember first = member(1L, USER_ID, "2222222222", false);
        FamilyMember second = member(2L, USER_ID, "3333333333", false);
        when(familyMemberRepository.insert(USER_ID, "2222222222", "identity/a.jpg"))
                .thenReturn(first);
        when(familyMemberRepository.insert(USER_ID, "3333333333", "identity/a.jpg"))
                .thenReturn(second);

        List<FamilyMember> result =
                familyMemberService.addMembers(
                        USER_ID, List.of("2222222222", " 3333333333 "), List.of(photo(), photo()));

        assertThat(result).containsExactly(first, second);
    }

    @Test
    void addMembers_withNniOfAnotherFamily_isRefusedBeforeStoringAnything() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni("2222222222"))
                .thenReturn(Optional.of(member(5L, OTHER_USER_ID, "2222222222", true)));

        assertThatThrownBy(
                        () ->
                                familyMemberService.addMembers(
                                        USER_ID, List.of("2222222222"), List.of(photo())))
                .isInstanceOf(UserException.NniAlreadyUsedException.class);
        verify(fileStorageService, never()).store(any(), any());
    }

    @Test
    void addMembers_withNniOfAnotherAccount_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni("2222222222")).thenReturn(Optional.empty());
        when(authRepository.existsOtherActiveUserWithNni("2222222222", USER_ID)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                familyMemberService.addMembers(
                                        USER_ID, List.of("2222222222"), List.of(photo())))
                .isInstanceOf(UserException.NniAlreadyUsedException.class);
    }

    @Test
    void addMembers_withOwnNni_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));

        assertThatThrownBy(
                        () ->
                                familyMemberService.addMembers(
                                        USER_ID, List.of("1111111111"), List.of(photo())))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);
    }

    @Test
    void addMembers_withSameNniTwice_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        when(familyMemberRepository.findByNni("2222222222")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                familyMemberService.addMembers(
                                        USER_ID,
                                        List.of("2222222222", "2222222222"),
                                        List.of(photo(), photo())))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);
    }

    @Test
    void addMembers_withNniNotTenDigits_isRefused() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));

        assertThatThrownBy(
                        () ->
                                familyMemberService.addMembers(
                                        USER_ID, List.of("12345"), List.of(photo())))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);
    }

    @Test
    void addMembers_withMemberAlreadyInThisFamily_reusesItWithoutNewPhoto() {
        when(authRepository.findById(USER_ID)).thenReturn(Optional.of(owner("1111111111")));
        FamilyMember known = member(3L, USER_ID, "2222222222", true);
        when(familyMemberRepository.findByNni("2222222222")).thenReturn(Optional.of(known));

        List<FamilyMember> result =
                familyMemberService.addMembers(USER_ID, List.of("2222222222"), List.of(photo()));

        assertThat(result).containsExactly(known);
        verify(fileStorageService, never()).store(any(), any());
        verify(familyMemberRepository, never()).insert(any(), any(), any());
    }

    @Test
    void reject_deletesTheMemberAndNotifiesTheOwner() {
        when(familyMemberRepository.findById(4L))
                .thenReturn(Optional.of(member(4L, USER_ID, "2222222222", false)));

        familyMemberService.reject(4L);

        verify(familyMemberRepository).deleteById(4L);
        verify(notificationService).create(eq(USER_ID), anyString(), anyString());
    }

    @Test
    void approve_whenAlreadyVerified_isRefused() {
        when(familyMemberRepository.findById(4L))
                .thenReturn(Optional.of(member(4L, USER_ID, "2222222222", true)));

        assertThatThrownBy(() -> familyMemberService.approve(4L))
                .isInstanceOf(UserException.IdentityNotPendingException.class);
    }
}
