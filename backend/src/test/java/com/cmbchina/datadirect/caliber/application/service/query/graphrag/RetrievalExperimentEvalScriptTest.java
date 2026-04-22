package com.cmbchina.datadirect.caliber.application.service.query.graphrag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrievalExperimentEvalScriptTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRenderDryRunCommandForNonExecutableMavenScript() throws Exception {
        Path fakeMavenScript = tempDir.resolve("fake-mvn");
        Files.writeString(fakeMavenScript, "#!/usr/bin/env bash\nexit 0\n", StandardCharsets.UTF_8);
        fakeMavenScript.toFile().setExecutable(false, false);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash",
                resolveScriptPath().toString(),
                "--dry-run",
                "--snapshot-id", "demo-snapshot",
                "--adapter", "LightRAG",
                "--gray-scope", "shadow-only"
        )
                .directory(resolveBackendDir().toFile())
                .redirectErrorStream(true);
        processBuilder.environment().put("MVN_BIN", fakeMavenScript.toString());
        Process process = processBuilder.start();

        String output = readAll(process.getInputStream());
        int exitCode = process.waitFor();

        assertThat(exitCode).isZero();
        assertThat(output).contains("cd " + resolveBackendDir());
        assertThat(output).contains("sh " + fakeMavenScript);
        assertThat(output).contains("exec:java");
        assertThat(output).contains("-Dexec.mainClass=com.cmbchina.datadirect.caliber.application.service.query.graphrag.RetrievalExperimentEvaluationService");
        assertThat(output).contains("--snapshot-id\\ demo-snapshot");
        assertThat(output).contains("--adapter\\ LightRAG");
        assertThat(output).contains("--gray-scope\\ shadow-only");
    }

    @Test
    void shouldOmitExecArgsWhenDryRunUsesDefaults() throws Exception {
        Path fakeMavenBinary = tempDir.resolve("fake-mvn-exec");
        Files.writeString(fakeMavenBinary, "#!/usr/bin/env bash\nexit 0\n", StandardCharsets.UTF_8);
        fakeMavenBinary.toFile().setExecutable(true, false);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "bash",
                resolveScriptPath().toString(),
                "--dry-run"
        )
                .directory(resolveBackendDir().toFile())
                .redirectErrorStream(true);
        processBuilder.environment().put("MVN_BIN", fakeMavenBinary.toString());
        Process process = processBuilder.start();

        String output = readAll(process.getInputStream());
        int exitCode = process.waitFor();

        assertThat(exitCode).isZero();
        assertThat(output).contains(fakeMavenBinary.toString());
        assertThat(output).doesNotContain("-Dexec.args=");
    }

    private Path resolveScriptPath() {
        Path backendDir = resolveBackendDir();
        Path script = backendDir.getParent().resolve("scripts/run_retrieval_experiment_eval.sh").normalize();
        assertThat(Files.exists(script)).isTrue();
        return script;
    }

    private Path resolveBackendDir() {
        Path workingDir = Path.of("").toAbsolutePath().normalize();
        if (Files.exists(workingDir.resolve("pom.xml")) && Files.isDirectory(workingDir.resolve("src"))) {
            return workingDir;
        }
        List<Path> candidates = List.of(
                workingDir.resolve("backend"),
                workingDir.getParent() == null ? workingDir : workingDir.getParent().resolve("backend")
        );
        return candidates.stream()
                .filter(path -> Files.exists(path.resolve("pom.xml")))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Unable to locate backend module from " + workingDir));
    }

    private String readAll(InputStream inputStream) throws IOException {
        try (inputStream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            inputStream.transferTo(outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }
}
