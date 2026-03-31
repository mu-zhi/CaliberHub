package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMysqlCompatibilityTest {

    private static final List<String> MYSQL_INCOMPATIBLE_TOKENS = List.of(
            "CLOB",
            "CREATE INDEX IF NOT EXISTS",
            "ADD COLUMN IF NOT EXISTS"
    );

    @Test
    void migrationScriptsShouldAvoidKnownMysqlIncompatibleSyntax() throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/resources/db/migration"))) {
            List<Path> files = stream
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();

            assertThat(files).isNotEmpty();
            for (Path file : files) {
                String sql = Files.readString(file, StandardCharsets.UTF_8);
                for (String token : MYSQL_INCOMPATIBLE_TOKENS) {
                    assertThat(sql)
                            .as("migration %s should stay mysql-compatible", file.getFileName())
                            .doesNotContain(token);
                }
            }
        }
    }
}
