package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AdminTripPackageResponse;
import com.omraty.backend.dto.response.TripPackageResponse;
import com.omraty.backend.entities.TripPackage;
import com.omraty.backend.service.TripPackageWithImages;
import java.util.List;

public final class TripPackageMapper {

    private TripPackageMapper() {}

    public static TripPackageResponse toResponse(TripPackageWithImages packageWithImages) {
        TripPackage pkg = packageWithImages.tripPackage();
        return new TripPackageResponse(
                String.valueOf(pkg.id()),
                pkg.title(),
                pkg.destination(),
                pkg.category(),
                pkg.price(),
                pkg.startDate(),
                pkg.endDate(),
                packageWithImages.imageUrls(),
                pkg.description(),
                pkg.includesVisa(),
                pkg.groupSize());
    }

    public static List<TripPackageResponse> toResponseList(
            List<TripPackageWithImages> packagesWithImages) {
        return packagesWithImages.stream().map(TripPackageMapper::toResponse).toList();
    }

    public static AdminTripPackageResponse toAdminResponse(
            TripPackageWithImages packageWithImages) {
        TripPackage pkg = packageWithImages.tripPackage();
        return new AdminTripPackageResponse(
                pkg.id(),
                pkg.title(),
                pkg.destination(),
                pkg.category(),
                pkg.price(),
                pkg.startDate(),
                pkg.endDate(),
                packageWithImages.imageUrls(),
                pkg.description(),
                pkg.includesVisa(),
                pkg.groupSize(),
                pkg.visible());
    }
}
