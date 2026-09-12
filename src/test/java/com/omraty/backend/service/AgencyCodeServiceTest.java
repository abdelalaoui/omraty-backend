package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.AgencyCode;
import com.omraty.backend.exception.AgencyCodeException;
import com.omraty.backend.repository.AgencyCodeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgencyCodeServiceTest {

    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @Mock private AgencyCodeRepository agencyCodeRepository;

    private AgencyCodeService agencyCodeService() {
        return new AgencyCodeService(agencyCodeRepository);
    }

    private AgencyCode agencyCode(String code, boolean used, UUID accountId) {
        return new AgencyCode(
                1L,
                "Agence Test",
                "+2223456789",
                new BigDecimal("10.00"),
                code,
                used,
                accountId,
                LocalDateTime.now());
    }

    @Test
    void getAgencyCodes_returnsAllCodesFromRepository_mostRecentFirst() {
        AgencyCode used = agencyCode("ABCD2345", true, UUID.randomUUID());
        AgencyCode unused = agencyCode("EFGH6789", false, null);
        when(agencyCodeRepository.findAll()).thenReturn(List.of(used, unused));

        List<AgencyCode> result = agencyCodeService().getAgencyCodes();

        assertThat(result).containsExactly(used, unused);
    }

    @Test
    void createAgencyCode_withBlankAgencyName_throwsException() {
        assertThatThrownBy(
                        () ->
                                agencyCodeService()
                                        .createAgencyCode(" ", "+2223456789", new BigDecimal("10")))
                .isInstanceOf(AgencyCodeException.InvalidAgencyCodeRequestException.class);
    }

    @Test
    void createAgencyCode_withZeroDiscountPercentage_throwsException() {
        assertThatThrownBy(
                        () ->
                                agencyCodeService()
                                        .createAgencyCode(
                                                "Agence Test", "+2223456789", BigDecimal.ZERO))
                .isInstanceOf(AgencyCodeException.InvalidAgencyCodeRequestException.class);
    }

    @Test
    void createAgencyCode_withDiscountPercentageAbove100_throwsException() {
        assertThatThrownBy(
                        () ->
                                agencyCodeService()
                                        .createAgencyCode(
                                                "Agence Test",
                                                "+2223456789",
                                                new BigDecimal("100.01")))
                .isInstanceOf(AgencyCodeException.InvalidAgencyCodeRequestException.class);
    }

    @Test
    void createAgencyCode_generatesAnUnusedUppercaseCodeAndDelegatesToRepository() {
        when(agencyCodeRepository.existsByCode(anyString())).thenReturn(false);
        AgencyCode created = agencyCode("ABCD2345", false, null);
        when(agencyCodeRepository.insert(
                        eq("Agence Test"),
                        eq("+2223456789"),
                        eq(new BigDecimal("10.00")),
                        anyString()))
                .thenReturn(created);

        AgencyCode result =
                agencyCodeService()
                        .createAgencyCode("Agence Test", "+2223456789", new BigDecimal("10.00"));

        assertThat(result.code()).isEqualTo("ABCD2345");
        assertThat(result.used()).isFalse();
        assertThat(result.accountId()).isNull();
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(agencyCodeRepository)
                .insert(
                        eq("Agence Test"),
                        eq("+2223456789"),
                        eq(new BigDecimal("10.00")),
                        codeCaptor.capture());
        String generatedCode = codeCaptor.getValue();
        assertThat(generatedCode).hasSize(8);
        assertThat(generatedCode).isEqualTo(generatedCode.toUpperCase());
    }

    @Test
    void createAgencyCode_whenFirstCandidateCollides_retriesUntilAnUnusedCodeIsFound() {
        when(agencyCodeRepository.existsByCode(anyString())).thenReturn(true, false);
        when(agencyCodeRepository.insert(
                        anyString(), anyString(), org.mockito.ArgumentMatchers.any(), anyString()))
                .thenReturn(agencyCode("EFGH6789", false, null));

        agencyCodeService().createAgencyCode("Agence Test", "+2223456789", new BigDecimal("10"));

        verify(agencyCodeRepository, times(2)).existsByCode(anyString());
    }

    @Test
    void verifyCode_whenCodeNotFound_throwsException() {
        when(agencyCodeRepository.findByCodeForUpdate("ABCD2345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agencyCodeService().verifyCode(ACCOUNT_ID, "ABCD2345"))
                .isInstanceOf(AgencyCodeException.AgencyCodeNotFoundException.class);
    }

    @Test
    void verifyCode_whenAlreadyUsed_throwsExceptionWithoutUpdating() {
        when(agencyCodeRepository.findByCodeForUpdate("ABCD2345"))
                .thenReturn(Optional.of(agencyCode("ABCD2345", true, UUID.randomUUID())));

        assertThatThrownBy(() -> agencyCodeService().verifyCode(ACCOUNT_ID, "ABCD2345"))
                .isInstanceOf(AgencyCodeException.AgencyCodeAlreadyUsedException.class);

        verify(agencyCodeRepository, never())
                .updateVerify(
                        org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void verifyCode_normalizesInputAndLinksAccountOnSuccess() {
        when(agencyCodeRepository.findByCodeForUpdate("ABCD2345"))
                .thenReturn(Optional.of(agencyCode("ABCD2345", false, null)));
        when(agencyCodeRepository.updateVerify(1L, ACCOUNT_ID))
                .thenReturn(agencyCode("ABCD2345", true, ACCOUNT_ID));

        AgencyCode result = agencyCodeService().verifyCode(ACCOUNT_ID, " abcd2345 ");

        assertThat(result.used()).isTrue();
        assertThat(result.accountId()).isEqualTo(ACCOUNT_ID);
    }
}
