package com.cmbchina.datadirect.caliber.infrastructure.module.dao.po;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SourceMaterialSchemaContractTest {

    @Test
    void shouldDeclareLongTextForSourceMaterialRawText() throws Exception {
        Field rawTextField = SourceMaterialPO.class.getDeclaredField("rawText");
        Column column = rawTextField.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.columnDefinition()).isEqualTo("LONGTEXT");
    }

    @Test
    void shouldProvideMigrationToExpandSourceMaterialRawTextToLongText() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V17__expand_source_material_raw_text.sql");

        assertThat(Files.exists(migration)).isTrue();
        String sql = Files.readString(migration);
        assertThat(sql).contains("caliber_source_material");
        assertThat(sql.toUpperCase()).contains("LONGTEXT");
    }
}
