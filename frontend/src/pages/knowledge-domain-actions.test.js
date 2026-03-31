import { describe, expect, it, vi } from "vitest";
import { reloadDomainsWithBootstrap } from "./knowledge-domain-actions";

describe("reloadDomainsWithBootstrap", () => {
  it("bootstraps and reloads domains", async () => {
    const loadDomains = vi.fn().mockResolvedValue([{ id: 1, domainName: "零售基础业务" }]);
    const bootstrapDomains = vi.fn().mockResolvedValue({ createdCount: 5, totalCount: 5 });

    const result = await reloadDomainsWithBootstrap({
      loadDomains,
      bootstrapDomains,
    });

    expect(bootstrapDomains).toHaveBeenCalledTimes(1);
    expect(loadDomains).toHaveBeenCalledTimes(1);
    expect(result.domains).toEqual([{ id: 1, domainName: "零售基础业务" }]);
    expect(result.bootstrapped).toBe(true);
    expect(result.bootstrapResult).toEqual({ createdCount: 5, totalCount: 5 });
  });

  it("still reloads when bootstrap is idempotent", async () => {
    const existingDomains = [{ id: 8, domainName: "公司业务" }];
    const loadDomains = vi.fn().mockResolvedValue(existingDomains);
    const bootstrapDomains = vi.fn().mockResolvedValue({ createdCount: 0, totalCount: 8 });

    const result = await reloadDomainsWithBootstrap({
      loadDomains,
      bootstrapDomains,
    });

    expect(bootstrapDomains).toHaveBeenCalledTimes(1);
    expect(loadDomains).toHaveBeenCalledTimes(1);
    expect(result).toEqual({
      domains: existingDomains,
      bootstrapped: false,
      bootstrapResult: { createdCount: 0, totalCount: 8 },
    });
  });
});
