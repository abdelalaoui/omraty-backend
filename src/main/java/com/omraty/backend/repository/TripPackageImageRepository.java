package com.omraty.backend.repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TripPackageImageRepository {

    private final JdbcTemplate jdbcTemplate;

    public TripPackageImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** URLs d'un package, dans l'ordre d'affichage (voir PackageModel.imageUrls). */
    public List<String> findUrlsByPackageId(long packageId) {
        return jdbcTemplate.queryForList(
                TripPackageImageTable.SELECT_URLS_BY_PACKAGE_ID, String.class, packageId);
    }

    /** URLs de plusieurs packages, groupées par package_id, pour la liste du catalogue (GET). */
    public Map<Long, List<String>> findUrlsByPackageIds(List<Long> packageIds) {
        if (packageIds.isEmpty()) {
            return Map.of();
        }
        return jdbcTemplate
                .query(
                        TripPackageImageTable.SELECT_BY_PACKAGE_IDS,
                        (PreparedStatement ps) -> {
                            Array array =
                                    ps.getConnection()
                                            .createArrayOf(
                                                    "bigint", packageIds.toArray(new Long[0]));
                            ps.setArray(1, array);
                        },
                        (rs, rowNum) -> Map.entry(rs.getLong("package_id"), rs.getString("url")))
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Map.Entry::getKey,
                                Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
    }

    /**
     * Remplace toutes les images d'un package par la liste fournie, dans l'ordre (display_order =
     * position dans la liste). À appeler dans la même transaction que la création/mise à jour du
     * package.
     */
    public void replaceImages(long packageId, List<String> urls) {
        jdbcTemplate.update(TripPackageImageTable.DELETE_BY_PACKAGE_ID, packageId);
        if (urls.isEmpty()) {
            return;
        }
        List<Object[]> batchArgs =
                IntStream.range(0, urls.size())
                        .mapToObj(index -> new Object[] {packageId, urls.get(index), index})
                        .toList();
        jdbcTemplate.batchUpdate(TripPackageImageTable.INSERT_IMAGE, batchArgs);
    }
}
