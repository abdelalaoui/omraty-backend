package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.User;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.exception.UserException;
import com.omraty.backend.repository.AuthRepository;
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
class UserServiceTest {

    private static final String NNI = "A123456";

    @Mock private AuthRepository authRepository;
    @Mock private FileStorageService fileStorageService;

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(authRepository, fileStorageService);
        user =
                new User(
                        UUID.randomUUID(),
                        "+212600000000",
                        "hashed-password",
                        "MALE",
                        null,
                        null,
                        false,
                        LocalDateTime.now(),
                        "USER");
    }

    private MultipartFile validPhoto() {
        return new MockMultipartFile(
                "photo", "id.jpg", "image/jpeg", "fake-image-bytes".getBytes());
    }

    @Test
    void updateIdentity_withBlankNni_throwsException() {
        assertThatThrownBy(() -> userService.updateIdentity(user.id(), "  ", validPhoto()))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);

        verify_neverStoresOrUpdates();
    }

    @Test
    void updateIdentity_withEmptyPhoto_throwsException() {
        MultipartFile emptyPhoto = new MockMultipartFile("photo", new byte[0]);

        assertThatThrownBy(() -> userService.updateIdentity(user.id(), NNI, emptyPhoto))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);

        verify_neverStoresOrUpdates();
    }

    @Test
    void updateIdentity_withUnsupportedContentType_throwsException() {
        MultipartFile pdfFile =
                new MockMultipartFile(
                        "photo", "id.pdf", "application/pdf", "fake-pdf-bytes".getBytes());

        assertThatThrownBy(() -> userService.updateIdentity(user.id(), NNI, pdfFile))
                .isInstanceOf(UserException.InvalidIdentityRequestException.class);

        verify_neverStoresOrUpdates();
    }

    @Test
    void updateIdentity_whenUserNotFound_throwsException() {
        MultipartFile photo = validPhoto();
        when(fileStorageService.store(photo, "identity")).thenReturn("/uploads/identity/x.jpg");
        when(authRepository.updateIdentity(user.id(), NNI, "/uploads/identity/x.jpg", false))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateIdentity(user.id(), NNI, photo))
                .isInstanceOf(AuthException.InvalidTokenException.class);
    }

    @Test
    void updateIdentity_success_storesPhotoAndUpdatesUserButKeepsIdentityUnverified() {
        MultipartFile photo = validPhoto();
        User updatedUser =
                new User(
                        user.id(),
                        user.phone(),
                        user.passwordHash(),
                        user.gender(),
                        NNI,
                        "/uploads/identity/x.jpg",
                        false,
                        user.createdAt(),
                        user.role());
        when(fileStorageService.store(photo, "identity")).thenReturn("/uploads/identity/x.jpg");
        when(authRepository.updateIdentity(user.id(), NNI, "/uploads/identity/x.jpg", false))
                .thenReturn(Optional.of(updatedUser));

        User result = userService.updateIdentity(user.id(), NNI, photo);

        assertThat(result).isEqualTo(updatedUser);
        assertThat(result.identityVerified()).isFalse();
    }

    private void verify_neverStoresOrUpdates() {
        verify(fileStorageService, never()).store(any(), anyString());
        verify(authRepository, never())
                .updateIdentity(any(), anyString(), anyString(), anyBoolean());
    }

    private User pendingUser() {
        return new User(
                user.id(),
                user.phone(),
                user.passwordHash(),
                user.gender(),
                NNI,
                "/uploads/identity/x.jpg",
                false,
                user.createdAt(),
                user.role());
    }

    @Test
    void listPendingIdentityVerifications_returnsRepositoryResult() {
        List<User> pending = List.of(pendingUser());
        when(authRepository.findPendingIdentityVerifications()).thenReturn(pending);

        assertThat(userService.listPendingIdentityVerifications()).isEqualTo(pending);
    }

    @Test
    void approveIdentity_whenNoPendingRequest_throwsException() {
        when(authRepository.findById(user.id())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.approveIdentity(user.id()))
                .isInstanceOf(UserException.IdentityNotPendingException.class);

        verify(authRepository, never()).approveIdentity(any());
    }

    @Test
    void approveIdentity_whenUserUnknown_throwsException() {
        when(authRepository.findById(user.id())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.approveIdentity(user.id()))
                .isInstanceOf(UserException.UserNotFoundException.class);

        verify(authRepository, never()).approveIdentity(any());
    }

    @Test
    void approveIdentity_success_marksIdentityVerified() {
        User pending = pendingUser();
        User approved =
                new User(
                        pending.id(),
                        pending.phone(),
                        pending.passwordHash(),
                        pending.gender(),
                        pending.nni(),
                        pending.idPhotoUrl(),
                        true,
                        pending.createdAt(),
                        pending.role());
        when(authRepository.findById(pending.id())).thenReturn(Optional.of(pending));
        when(authRepository.approveIdentity(pending.id())).thenReturn(Optional.of(approved));

        User result = userService.approveIdentity(pending.id());

        assertThat(result.identityVerified()).isTrue();
    }

    @Test
    void rejectIdentity_success_clearsSubmittedNniAndPhoto() {
        User pending = pendingUser();
        User rejected =
                new User(
                        pending.id(),
                        pending.phone(),
                        pending.passwordHash(),
                        pending.gender(),
                        null,
                        null,
                        false,
                        pending.createdAt(),
                        pending.role());
        when(authRepository.findById(pending.id())).thenReturn(Optional.of(pending));
        when(authRepository.rejectIdentity(pending.id())).thenReturn(Optional.of(rejected));

        User result = userService.rejectIdentity(pending.id());

        assertThat(result.nni()).isNull();
        assertThat(result.idPhotoUrl()).isNull();
    }
}
