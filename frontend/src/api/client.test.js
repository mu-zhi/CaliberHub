import { describe, expect, it } from "vitest";
import { parseJsonText } from "./client";

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
