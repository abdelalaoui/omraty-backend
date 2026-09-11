package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.PackageResponse;
import com.omraty.backend.entities.OmraPackage;
import java.util.List;

public final class PackageMapper {

    private PackageMapper() {}

    public static PackageResponse toResponse(OmraPackage pkg) {
        return new PackageResponse(pkg.id(), pkg.groupSize());
    }

    public static List<PackageResponse> toResponseList(List<OmraPackage> packages) {
        return packages.stream().map(PackageMapper::toResponse).toList();
    }
}
