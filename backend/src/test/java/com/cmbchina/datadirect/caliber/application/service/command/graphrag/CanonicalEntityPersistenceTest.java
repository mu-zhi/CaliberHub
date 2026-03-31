package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CanonicalEntityTestApplication.class)
@ActiveProfiles("test")
@Transactional
class CanonicalEntityPersistenceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistCanonicalEntityAndMembership() {
        Timestamp now = Timestamp.from(OffsetDateTime.now().toInstant());

        jdbcTemplate.update("""
                INSERT INTO caliber_canonical_entity (
                    entity_type,
                    canonical_key,
                    display_name,
                    resolution_status,
                    lifecycle_status,
                    profile_json,
                    created_by,
                    created_at,
                    updated_by,
                    updated_at,
                    row_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                "SOURCE_CONTRACT",
                "SRC::PAYROLL::T05_AGN_DTL",
                "T05_AGN_DTL",
                "ACTIVE",
                "ACTIVE",
                "{\"normalizedPhysicalTable\":\"T05_AGN_DTL\"}",
                "tester",
                now,
                "tester",
                now,
                0L
        );

        Long canonicalEntityId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM caliber_canonical_entity
                WHERE entity_type = ? AND canonical_key = ?
                """, Long.class, "SOURCE_CONTRACT", "SRC::PAYROLL::T05_AGN_DTL");

        jdbcTemplate.update("""
                INSERT INTO caliber_canonical_entity_membership (
                    canonical_entity_id,
                    scene_asset_type,
                    scene_asset_id,
                    scene_id,
                    match_basis,
                    confidence_score,
                    manual_override,
                    active_flag,
                    created_by,
                    created_at,
                    updated_by,
                    updated_at,
                    row_version
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                canonicalEntityId,
                "SOURCE_CONTRACT",
                101L,
                12L,
                "physical_table",
                1.0d,
                false,
                true,
                "tester",
                now,
                "tester",
                now,
                0L
        );

        Integer membershipCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM caliber_canonical_entity_membership
                WHERE canonical_entity_id = ?
                  AND scene_asset_type = ?
                  AND scene_asset_id = ?
                """, Integer.class, canonicalEntityId, "SOURCE_CONTRACT", 101L);

        assertThat(canonicalEntityId).isNotNull();
        assertThat(membershipCount).isEqualTo(1);
    }
}
