import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");

function readRepoFile(relativePath) {
  return readFileSync(path.join(repoRoot, relativePath), "utf8");
}

describe("repository runtime contracts", () => {
  it("keeps backend security defaults fail-safe in application.yml", () => {
    const applicationYaml = readRepoFile("backend/src/main/resources/application.yml");

    expect(applicationYaml).toContain("enabled: ${CALIBER_SECURITY_ENABLED:true}");
    expect(applicationYaml).toContain("require-write-auth: ${CALIBER_REQUIRE_WRITE_AUTH:true}");
    expect(applicationYaml).not.toContain("enabled: ${CALIBER_SECURITY_ENABLED:false}");
    expect(applicationYaml).not.toContain("require-write-auth: ${CALIBER_REQUIRE_WRITE_AUTH:false}");
  });

  it("keeps CI on the Maven path and aligned with backend port 8082", () => {
    const ciWorkflow = readRepoFile(".github/workflows/ci.yml");
    const ciBaselineWorkflow = readRepoFile(".github/workflows/ci-baseline.yml");

    expect(ciWorkflow).toContain("cache: maven");
    expect(ciWorkflow).toContain("run: mvn -q test");
    expect(ciWorkflow).not.toContain("cache: gradle");
    expect(ciWorkflow).not.toContain("./gradlew test");

    expect(ciBaselineWorkflow).toContain("http://127.0.0.1:8082/api/system/auth/token");
    expect(ciBaselineWorkflow).toContain("http://127.0.0.1:8082/actuator/health");
    expect(ciBaselineWorkflow).not.toContain("http://127.0.0.1:8080/");
  });

  it("keeps frontend dist sync as a pure copy step and aligns helper scripts to backend port 8082", () => {
    const syncScript = readRepoFile("scripts/sync_frontend_dist.sh");
    const systemFlowScript = readRepoFile("scripts/run_system_test_flow.sh");
    const nfrGateScript = readRepoFile("scripts/run_nfr_acceptance_gate.sh");

    expect(syncScript).not.toContain("npm run build");
    expect(systemFlowScript).toContain('BASE_URL="${BASE_URL:-http://127.0.0.1:8082}"');
    expect(nfrGateScript).toContain('BASE_URL="${BASE_URL:-http://127.0.0.1:8082}"');
    expect(nfrGateScript).not.toContain('BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"');
  });
});
