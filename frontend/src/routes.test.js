import { describe, expect, it } from "vitest";
import {
  LEGACY_ROUTE_REDIRECTS,
  SIDE_ROUTES,
  findRoute,
  isTopModuleAccessible,
} from "./routes";

describe("route governance", () => {
  it("keeps legacy redirects out of the formal side route registry", () => {
    const formalPaths = SIDE_ROUTES.map((item) => item.path);
    expect(formalPaths.some((path) => path.startsWith("/assets"))).toBe(false);
    expect(formalPaths.some((path) => path.startsWith("/knowledge"))).toBe(false);
    expect(LEGACY_ROUTE_REDIRECTS["/assets/map"]).toBe("/map");
    expect(LEGACY_ROUTE_REDIRECTS["/knowledge/import"]).toBe("/production/ingest");
  });

  it("marks prototype and sample routes explicitly", () => {
    expect(findRoute("/prototype")?.maturity).toBe("prototype");
    expect(findRoute("/prototype/runtime-publish")?.maturity).toBe("prototype");
    expect(findRoute("/approval")?.maturity).toBe("sample");
    expect(findRoute("/monitoring")?.maturity).toBe("sample");
  });

  it("computes top-module access by role", () => {
    expect(isTopModuleAccessible("publish", "governance")).toBe(true);
    expect(isTopModuleAccessible("approval", "compliance")).toBe(true);
    expect(isTopModuleAccessible("approval", "support")).toBe(false);
    expect(isTopModuleAccessible("publish", "frontline")).toBe(false);
  });
});
