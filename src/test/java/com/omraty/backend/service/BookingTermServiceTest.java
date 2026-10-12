package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.BookingTerm;
import com.omraty.backend.exception.BookingTermException;
import com.omraty.backend.repository.BookingTermRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingTermServiceTest {

    @Mock private BookingTermRepository bookingTermRepository;

    private BookingTermService bookingTermService() {
        return new BookingTermService(bookingTermRepository);
    }

    private BookingTerm term(long id, String textAr, boolean enabled, int displayOrder) {
        return new BookingTerm(
                id,
                textAr,
                null,
                null,
                displayOrder,
                enabled,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    @Test
    void getActiveTerms_returnsRepositoryResult() {
        List<BookingTerm> terms = List.of(term(1L, "شرط 1", true, 1));
        when(bookingTermRepository.findActiveTerms()).thenReturn(terms);

        assertThat(bookingTermService().getActiveTerms()).isEqualTo(terms);
    }

    @Test
    void getAllTerms_returnsRepositoryResult() {
        List<BookingTerm> terms = List.of(term(1L, "شرط 1", true, 1), term(2L, "شرط 2", false, 2));
        when(bookingTermRepository.findAllTerms()).thenReturn(terms);

        assertThat(bookingTermService().getAllTerms()).isEqualTo(terms);
    }

    @Test
    void getCurrentVersion_usesMaxUpdatedAtAsOpaqueToken() {
        LocalDateTime maxUpdatedAt = LocalDateTime.of(2026, 3, 5, 10, 15, 30);
        when(bookingTermRepository.findMaxUpdatedAt()).thenReturn(Optional.of(maxUpdatedAt));

        assertThat(bookingTermService().getCurrentVersion()).isEqualTo(maxUpdatedAt.toString());
    }

    @Test
    void getCurrentVersion_whenNoTermsExist_returnsStableFallback() {
        when(bookingTermRepository.findMaxUpdatedAt()).thenReturn(Optional.empty());

        assertThat(bookingTermService().getCurrentVersion()).isEqualTo("0");
    }

    @Test
    void createTerm_whenTextArBlank_throwsExceptionWithoutInserting() {
        assertThatThrownBy(() -> bookingTermService().createTerm("   ", null, null, null, null))
                .isInstanceOf(BookingTermException.InvalidBookingTermRequestException.class);
        verify(bookingTermRepository, never()).insert(any(), any(), any(), anyInt(), anyBoolean());
    }

    @Test
    void createTerm_whenDisplayOrderNotProvided_usesNextDisplayOrder() {
        when(bookingTermRepository.nextDisplayOrder()).thenReturn(4);
        BookingTerm inserted = term(1L, "شرط جديد", true, 4);
        when(bookingTermRepository.insert("شرط جديد", null, null, 4, true)).thenReturn(inserted);

        BookingTerm result = bookingTermService().createTerm("شرط جديد", null, null, null, null);

        assertThat(result).isEqualTo(inserted);
    }

    @Test
    void createTerm_whenEnabledNotProvided_defaultsToTrue() {
        when(bookingTermRepository.nextDisplayOrder()).thenReturn(0);
        BookingTerm inserted = term(1L, "شرط", true, 0);
        when(bookingTermRepository.insert("شرط", null, null, 0, true)).thenReturn(inserted);

        bookingTermService().createTerm("شرط", null, null, null, null);

        verify(bookingTermRepository).insert("شرط", null, null, 0, true);
    }

    @Test
    void updateTerm_whenNotFound_throwsException() {
        when(bookingTermRepository.update(1L, "نص جديد", null, null, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingTermService().updateTerm(1L, "نص جديد", null, null, null))
                .isInstanceOf(BookingTermException.BookingTermNotFoundException.class);
    }

    @Test
    void updateTerm_onlyChangesProvidedFields() {
        BookingTerm updated = term(1L, "شرط", false, 0);
        when(bookingTermRepository.update(1L, null, null, null, false))
                .thenReturn(Optional.of(updated));

        BookingTerm result = bookingTermService().updateTerm(1L, null, null, null, false);

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void reorderTerms_withDuplicateIds_throwsExceptionWithoutQueryingRepository() {
        assertThatThrownBy(() -> bookingTermService().reorderTerms(List.of(1L, 2L, 1L)))
                .isInstanceOf(BookingTermException.InvalidBookingTermRequestException.class);
        verify(bookingTermRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void reorderTerms_whenIdDoesNotExist_throwsException() {
        when(bookingTermRepository.findById(1L))
                .thenReturn(Optional.of(term(1L, "شرط 1", true, 0)));
        when(bookingTermRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingTermService().reorderTerms(List.of(1L, 2L)))
                .isInstanceOf(BookingTermException.BookingTermNotFoundException.class);
        verify(bookingTermRepository, never()).updateDisplayOrders(any());
    }

    @Test
    void reorderTerms_withValidIds_updatesDisplayOrders() {
        when(bookingTermRepository.findById(2L))
                .thenReturn(Optional.of(term(2L, "شرط 2", true, 1)));
        when(bookingTermRepository.findById(1L))
                .thenReturn(Optional.of(term(1L, "شرط 1", true, 0)));

        bookingTermService().reorderTerms(List.of(2L, 1L));

        verify(bookingTermRepository).updateDisplayOrders(List.of(2L, 1L));
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static boolean anyBoolean() {
        return org.mockito.ArgumentMatchers.anyBoolean();
    }
}
