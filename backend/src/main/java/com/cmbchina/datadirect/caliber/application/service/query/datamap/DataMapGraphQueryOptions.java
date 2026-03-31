package com.cmbchina.datadirect.caliber.application.service.query.datamap;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public record DataMapGraphQueryOptions(
        Long snapshotId,
        Set<String> objectTypes,
        Set<String> statuses,
        Set<String> relationTypes,
        Set<String> sensitivityScopes
) {

    public static DataMapGraphQueryOptions of(Long snapshotId,
                                              String objectTypes,
                                              String statuses,
                                              String relationTypes,
                                              String sensitivityScopes) {
        return new DataMapGraphQueryOptions(
                snapshotId,
                normalizeCsv(objectTypes),
                normalizeCsv(statuses),
                normalizeCsv(relationTypes),
                normalizeCsv(sensitivityScopes)
        );
    }

    private static Set<String> normalizeCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(item -> item == null ? "" : item.trim())
                .filter(item -> !item.isBlank())
                .map(item -> item.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
