/* @vitest-environment jsdom */

import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest, parseJsonText } from "./client";

afterEach(() => {
  vi.unstubAllGlobals();
  window.sessionStorage.clear();
});

describe("parseJsonText", () => {
  it("returns fallback when text is empty", () => {
    expect(parseJsonText("", { ok: false })).toEqual({ ok: false });
  });

  it("returns parsed json object", () => {
    expect(parseJsonText("{\"ok\":true}", { ok: false })).toEqual({ ok: true });
  });

  it("returns fallback when text is invalid", () => {
    expect(parseJsonText("{", { ok: false })).toEqual({ ok: false });
  });
});

describe("apiRequest", () => {
  it("auto-attaches the session token for non-auth api calls", async () => {
    window.sessionStorage.setItem("dd_auth_token", "session-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ ok: true }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest("/scenes");

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/scenes",
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: "Bearer session-token",
        }),
      }),
    );
  });

  it("does not attach the session token when requesting the auth endpoint", async () => {
    window.sessionStorage.setItem("dd_auth_token", "session-token");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ accessToken: "new-token" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest("/system/auth/token", { method: "POST", body: { username: "support", password: "pw" } });

    expect(fetchMock).toHaveBeenCalledWith(
      "/api/system/auth/token",
      expect.objectContaining({
        headers: expect.not.objectContaining({
          Authorization: "Bearer session-token",
        }),
      }),
    );
  });
});
