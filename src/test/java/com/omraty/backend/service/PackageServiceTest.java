package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.repository.PackageRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PackageServiceTest {

    @Mock private PackageRepository packageRepository;

    private PackageService packageService() {
        return new PackageService(packageRepository);
    }

    @Test
    void createPackage_withPositiveGroupSize_delegatesToRepository() {
        when(packageRepository.insert(40)).thenReturn(new OmraPackage(1L, 40));

        OmraPackage created = packageService().createPackage(40);

        assertThat(created.groupSize()).isEqualTo(40);
    }

    @Test
    void createPackage_withZeroGroupSize_throwsException() {
        assertThatThrownBy(() -> packageService().createPackage(0))
                .isInstanceOf(PackageException.InvalidPackageRequestException.class);
    }

    @Test
    void getPackageById_whenNotFound_throwsException() {
        when(packageRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> packageService().getPackageById(1L))
                .isInstanceOf(PackageException.PackageNotFoundException.class);
    }
}
