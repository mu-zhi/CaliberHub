package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DataSourceRuntimeConfigTest {

    @Test
    void shouldUseMysqlCompatibleDatasourceDefaultsOutsideTestProfile() throws Exception {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertThat(yaml).contains("url: ${CALIBER_DB_URL:jdbc:mysql://");
        assertThat(yaml).contains("driver-class-name: ${CALIBER_DB_DRIVER:com.mysql.cj.jdbc.Driver}");
        assertThat(yaml).contains("username: ${CALIBER_DB_USERNAME:caliber}");
    }
}
