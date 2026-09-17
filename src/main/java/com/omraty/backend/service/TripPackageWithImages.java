package com.omraty.backend.service;

import com.omraty.backend.entities.TripPackage;
import java.util.List;

/** Un package du catalogue avec ses images (table séparée, voir TripPackageImageRepository). */
public record TripPackageWithImages(TripPackage tripPackage, List<String> imageUrls) {}
