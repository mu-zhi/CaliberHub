package com.cmbchina.datadirect.caliber.application.service.command.graphrag;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalEntityGateScriptSmokeTest {

    @Test
    void shouldExposeCanonicalEntityGateScript() {
        Path script = Path.of(System.getProperty("user.dir"))
                .resolve("../scripts/run_canonical_entity_gate.sh")
                .normalize();

        assertThat(script).as("canonical gate script path").isRegularFile();
        assertThat(Files.isExecutable(script)).as("canonical gate script executable").isTrue();
        assertThat(runSyntaxCheck(script)).as("canonical gate script syntax").isEqualTo(0);
    }

    private int runSyntaxCheck(Path script) {
        try {
            Process process = new ProcessBuilder(List.of("bash", "-n", script.toString()))
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            assertThat(output).as("bash -n output").isBlank();
            return exitCode;
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("failed to inspect canonical gate script", e);
        }
    }
}
