package com.omraty.backend.service;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.repository.PackageRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PackageService {

    private final PackageRepository packageRepository;

    public PackageService(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public List<OmraPackage> getPackages() {
        return packageRepository.findAll();
    }

    public OmraPackage createPackage(int groupSize) {
        if (groupSize <= 0) {
            throw new PackageException.InvalidPackageRequestException(
                    "Le groupSize doit être positif");
        }
        return packageRepository.insert(groupSize);
    }

    /**
     * @throws PackageException.PackageNotFoundException si le package n'existe pas.
     */
    public OmraPackage getPackageById(long id) {
        return packageRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new PackageException.PackageNotFoundException(
                                        "Package introuvable (id=" + id + ")"));
    }
}
