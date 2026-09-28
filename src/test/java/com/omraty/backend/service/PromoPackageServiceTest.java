package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.PromoPackage;
import com.omraty.backend.entities.PromoPackageTier;
import com.omraty.backend.exception.PromoPackageException;
import com.omraty.backend.repository.PromoPackageRepository;
import com.omraty.backend.repository.PromoPackageTierRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromoPackageServiceTest {

    @Mock private PromoPackageRepository promoPackageRepository;
    @Mock private PromoPackageTierRepository promoPackageTierRepository;

    private PromoPackageService promoPackageService() {
        return new PromoPackageService(promoPackageRepository, promoPackageTierRepository);
    }

    private PromoPackage promoPackage(long id) {
        return new PromoPackage(
                id, "Offre flash", "Description", LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private PromoPackageTier tier(long id, long packageId, int type) {
        return new PromoPackageTier(id, packageId, type, type, new BigDecimal("1000.00"));
    }

    @Test
    void getPromoPackages_attachesTiersByPackageId() {
        PromoPackage pkg = promoPackage(1L);
        when(promoPackageRepository.findAll()).thenReturn(List.of(pkg));
        when(promoPackageTierRepository.findByPromoPackageIds(List.of(1L)))
                .thenReturn(Map.of(1L, List.of(tier(10L, 1L, 2))));

        List<PromoPackageWithTiers> result = promoPackageService().getPromoPackages();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).promoPackage()).isEqualTo(pkg);
        assertThat(result.get(0).tiers()).extracting(PromoPackageTier::id).containsExactly(10L);
    }

    @Test
    void getPromoPackageById_whenAbsent_throwsNotFound() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoPackageService().getPromoPackageById(1L))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);
    }

    @Test
    void createPromoPackage_withValidData_delegatesToRepository() {
        PromoPackage created = promoPackage(1L);
        when(promoPackageRepository.insert("Offre flash", "Description")).thenReturn(created);

        PromoPackageWithTiers result =
                promoPackageService().createPromoPackage("Offre flash", "Description");

        assertThat(result.promoPackage()).isEqualTo(created);
        assertThat(result.tiers()).isEmpty();
    }

    @Test
    void createPromoPackage_withBlankTitle_throwsExceptionWithoutTouchingRepository() {
        assertThatThrownBy(() -> promoPackageService().createPromoPackage("  ", null))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageRequestException.class);

        verify(promoPackageRepository, never()).insert(any(), any());
    }

    @Test
    void updatePromoPackage_whenNotFound_throwsException() {
        when(promoPackageRepository.update(1L, null, null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoPackageService().updatePromoPackage(1L, null, null))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);
    }

    @Test
    void updatePromoPackage_titleOnly_delegatesToRepository() {
        PromoPackage updated = promoPackage(1L);
        when(promoPackageRepository.update(1L, "Nouveau titre", null))
                .thenReturn(Optional.of(updated));
        when(promoPackageTierRepository.findByPromoPackageId(1L)).thenReturn(List.of());

        PromoPackageWithTiers result =
                promoPackageService().updatePromoPackage(1L, "Nouveau titre", null);

        assertThat(result.promoPackage()).isEqualTo(updated);
    }

    @Test
    void deletePromoPackage_whenNotFound_throwsException() {
        when(promoPackageRepository.deleteById(1L)).thenReturn(false);

        assertThatThrownBy(() -> promoPackageService().deletePromoPackage(1L))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);
    }

    @Test
    void deletePromoPackage_whenFound_delegatesToRepository() {
        when(promoPackageRepository.deleteById(1L)).thenReturn(true);

        promoPackageService().deletePromoPackage(1L);

        verify(promoPackageRepository).deleteById(1L);
    }

    @Test
    void addTier_withValidData_delegatesToRepository() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findByPromoPackageId(1L)).thenReturn(List.of());
        PromoPackageTier created = tier(10L, 1L, 2);
        when(promoPackageTierRepository.insert(1L, 2, 2, new BigDecimal("1000.00")))
                .thenReturn(created);

        PromoPackageTier result =
                promoPackageService().addTier(1L, 2, 2, new BigDecimal("1000.00"));

        assertThat(result).isEqualTo(created);
    }

    @Test
    void addTier_whenPackageNotFound_throwsException() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoPackageService().addTier(1L, 2, 2, new BigDecimal("1000.00")))
                .isInstanceOf(PromoPackageException.PromoPackageNotFoundException.class);

        verify(promoPackageTierRepository, never()).insert(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void addTier_withInvalidType_throwsException() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));

        assertThatThrownBy(() -> promoPackageService().addTier(1L, 4, 4, new BigDecimal("1000.00")))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageTierRequestException.class);
    }

    @Test
    void addTier_withNegativePrice_throwsException() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));

        assertThatThrownBy(() -> promoPackageService().addTier(1L, 2, 2, new BigDecimal("-1")))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageTierRequestException.class);
    }

    @Test
    void addTier_withDuplicateType_throwsException() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findByPromoPackageId(1L))
                .thenReturn(List.of(tier(10L, 1L, 2)));

        assertThatThrownBy(() -> promoPackageService().addTier(1L, 2, 2, new BigDecimal("1000.00")))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageTierRequestException.class);

        verify(promoPackageTierRepository, never()).insert(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void updateTier_withSameTypeAsItself_doesNotThrow() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        PromoPackageTier existing = tier(10L, 1L, 2);
        when(promoPackageTierRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(promoPackageTierRepository.findByPromoPackageId(1L)).thenReturn(List.of(existing));
        PromoPackageTier updated = tier(10L, 1L, 2);
        when(promoPackageTierRepository.update(10L, 2, null, null))
                .thenReturn(Optional.of(updated));

        PromoPackageTier result = promoPackageService().updateTier(1L, 10L, 2, null, null);

        assertThat(result).isEqualTo(updated);
    }

    @Test
    void updateTier_withTypeAlreadyUsedBySibling_throwsException() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        PromoPackageTier existing = tier(10L, 1L, 2);
        PromoPackageTier sibling = tier(11L, 1L, 3);
        when(promoPackageTierRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(promoPackageTierRepository.findByPromoPackageId(1L))
                .thenReturn(List.of(existing, sibling));

        assertThatThrownBy(() -> promoPackageService().updateTier(1L, 10L, 3, null, null))
                .isInstanceOf(PromoPackageException.InvalidPromoPackageTierRequestException.class);

        verify(promoPackageTierRepository, never()).update(anyLong(), any(), any(), any());
    }

    @Test
    void updateTier_whenTierBelongsToAnotherPackage_throwsNotFound() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findById(10L)).thenReturn(Optional.of(tier(10L, 2L, 2)));

        assertThatThrownBy(() -> promoPackageService().updateTier(1L, 10L, null, null, null))
                .isInstanceOf(PromoPackageException.PromoPackageTierNotFoundException.class);
    }

    @Test
    void deleteTier_whenFound_delegatesToRepository() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findById(10L)).thenReturn(Optional.of(tier(10L, 1L, 2)));

        promoPackageService().deleteTier(1L, 10L);

        verify(promoPackageTierRepository).deleteById(10L);
    }

    @Test
    void deleteTier_whenTierBelongsToAnotherPackage_throwsNotFoundWithoutDeleting() {
        when(promoPackageRepository.findById(1L)).thenReturn(Optional.of(promoPackage(1L)));
        when(promoPackageTierRepository.findById(10L)).thenReturn(Optional.of(tier(10L, 2L, 2)));

        assertThatThrownBy(() -> promoPackageService().deleteTier(1L, 10L))
                .isInstanceOf(PromoPackageException.PromoPackageTierNotFoundException.class);

        verify(promoPackageTierRepository, never()).deleteById(anyLong());
    }
}
