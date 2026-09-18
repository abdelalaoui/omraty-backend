package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.PurchaseResponse;
import com.omraty.backend.service.UserPurchase;
import java.util.List;

public final class PurchaseMapper {

    private PurchaseMapper() {}

    public static PurchaseResponse toResponse(UserPurchase purchase) {
        return new PurchaseResponse(
                purchase.type(),
                purchase.totalCapacity(),
                purchase.packageId(),
                purchase.packageLabel(),
                purchase.createdAt(),
                purchase.bedNumber());
    }

    public static List<PurchaseResponse> toResponseList(List<UserPurchase> purchases) {
        return purchases.stream().map(PurchaseMapper::toResponse).toList();
    }
}
