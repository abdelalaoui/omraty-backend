package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.Benefit;
import com.omraty.backend.exception.BenefitException;
import com.omraty.backend.repository.BenefitRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock private BenefitRepository benefitRepository;

    private BenefitService benefitService() {
        return new BenefitService(benefitRepository);
    }

    private Benefit benefit(long id, int displayOrder, boolean visible) {
        return new Benefit(
                id, "check-circle", "Meilleurs prix", displayOrder, visible, LocalDateTime.now());
    }

    @Test
    void getActiveBenefits_returnsBenefitsFromRepository() {
        List<Benefit> benefits = List.of(benefit(1L, 0, true), benefit(2L, 1, true));
        when(benefitRepository.findActiveBenefits()).thenReturn(benefits);

        assertThat(benefitService().getActiveBenefits()).isEqualTo(benefits);
    }

    @Test
    void createBenefit_withoutDisplayOrderOrVisible_defaultsToNextOrderAndVisible() {
        when(benefitRepository.nextDisplayOrder()).thenReturn(3);
        when(benefitRepository.insert("shield", "Sécurisé", 3, true))
                .thenReturn(benefit(1L, 3, true));

        Benefit created = benefitService().createBenefit("shield", "Sécurisé", null, null);

        assertThat(created.displayOrder()).isEqualTo(3);
        assertThat(created.visible()).isTrue();
    }

    @Test
    void createBenefit_withBlankLabel_throwsException() {
        assertThatThrownBy(() -> benefitService().createBenefit("shield", "  ", null, null))
                .isInstanceOf(BenefitException.InvalidBenefitRequestException.class);
    }

    @Test
    void updateBenefit_whenNotFound_throwsException() {
        when(benefitRepository.update(1L, null, null, null, false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService().updateBenefit(1L, null, null, null, false))
                .isInstanceOf(BenefitException.BenefitNotFoundException.class);
    }

    @Test
    void reorderBenefits_withDuplicateIds_throwsExceptionWithoutTouchingRepository() {
        assertThatThrownBy(() -> benefitService().reorderBenefits(List.of(1L, 2L, 1L)))
                .isInstanceOf(BenefitException.InvalidBenefitRequestException.class);

        verify(benefitRepository, never()).updateDisplayOrders(anyList());
    }

    @Test
    void reorderBenefits_whenIdDoesNotExist_throwsException() {
        when(benefitRepository.findById(1L)).thenReturn(Optional.of(benefit(1L, 0, true)));
        when(benefitRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> benefitService().reorderBenefits(List.of(1L, 2L)))
                .isInstanceOf(BenefitException.BenefitNotFoundException.class);
    }

    @Test
    void reorderBenefits_success_delegatesToRepository() {
        when(benefitRepository.findById(1L)).thenReturn(Optional.of(benefit(1L, 0, true)));
        when(benefitRepository.findById(2L)).thenReturn(Optional.of(benefit(2L, 1, true)));

        benefitService().reorderBenefits(List.of(2L, 1L));

        verify(benefitRepository).updateDisplayOrders(List.of(2L, 1L));
    }
}
