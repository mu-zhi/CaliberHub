# Frontend OpenAPI Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate a checked-in OpenAPI type snapshot from the backend and let the frontend consume typed contract paths instead of continuing to scatter handwritten endpoint strings.

**Architecture:** Keep the existing `apiRequest` runtime and add a thin contract layer on top of it. Backend remains the contract source via `/v3/api-docs`; frontend generates `openapi.d.ts`, defines typed path constants/builders, and migrates core pages/stores to those constants without rewriting the whole data layer.

**Tech Stack:** SpringDoc OpenAPI, `openapi-typescript`, Vite, React, Vitest, TypeScript type-check, JSDoc/typed path constants

---

## Task 1: Add a failing contract-layer test

**Files:**

- Create: `frontend/src/api/contracts.test.js`

- [ ] **Step 1: Write the failing test**

Create a test that imports a not-yet-existing contract module and asserts static and templated frontend paths:

```js
import { describe, expect, it } from "vitest";
import { API_CONTRACTS, buildApiPath } from "./contracts";

describe("OpenAPI contract paths", () => {
  it("maps static OpenAPI paths to frontend client paths", () => {
    expect(API_CONTRACTS.authToken).toBe("/system/auth/token");
    expect(API_CONTRACTS.scenes).toBe("/scenes");
    expect(API_CONTRACTS.graphQuery).toBe("/graphrag/query");
  });

  it("fills templated OpenAPI paths for frontend requests", () => {
    expect(buildApiPath("sceneById", { id: 12 })).toBe("/scenes/12");
    expect(buildApiPath("scenePublish", { id: "12" })).toBe("/scenes/12/publish");
    expect(buildApiPath("graphProjection", { sceneId: 8 })).toBe("/graphrag/projection/8");
  });
});
```

- [ ] **Step 2: Run the test to verify red**

Run: `cd frontend && npm test -- src/api/contracts.test.js`

Expected: FAIL because `./contracts` does not exist yet.

## Task 2: Generate OpenAPI types and implement the contract layer

**Files:**

- Modify: `frontend/package.json`
- Modify: `frontend/tsconfig.typecheck.json`
- Create: `frontend/scripts/generate-openapi-types.mjs`
- Create: `frontend/src/types/openapi.d.ts`
- Create: `frontend/src/api/contracts.ts`

- [ ] **Step 1: Add the generator dependency and script**

Add `openapi-typescript` as a frontend dev dependency and add a script like `generate:openapi` that runs `node ./scripts/generate-openapi-types.mjs`.

- [ ] **Step 2: Implement the generator**

Create `frontend/scripts/generate-openapi-types.mjs` so it:

```js
import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import openapiTS from "openapi-typescript";

const schemaUrl = process.env.OPENAPI_SCHEMA_URL || "http://127.0.0.1:8080/v3/api-docs";
const outputPath = path.resolve(process.cwd(), "src/types/openapi.d.ts");
const contents = await openapiTS(new URL(schemaUrl));
await fs.mkdir(path.dirname(outputPath), { recursive: true });
await fs.writeFile(outputPath, `// generated from ${schemaUrl}\\n${contents}`, "utf8");
```

- [ ] **Step 3: Implement the typed contract layer**

Create `frontend/src/api/contracts.ts` with:

```ts
import type { paths } from "../types/openapi";

type OpenApiPath = keyof paths & string;
type ClientPath<T extends string> = T extends `/api${infer Rest}` ? Rest : never;

function toClientPath<T extends OpenApiPath>(path: T): ClientPath<T> {
  return path.replace(/^\\/api/, "") as ClientPath<T>;
}

function fillPath<T extends OpenApiPath>(path: T, params: Record<string, string | number>): ClientPath<T> {
  let resolved = path;
  Object.entries(params).forEach(([key, value]) => {
    resolved = resolved.replace(new RegExp(`\\\\{${key}\\\\}`, "g"), encodeURIComponent(String(value))) as T;
  });
  return toClientPath(resolved);
}
```

Then expose:
- typed static constants such as `authToken`, `domains`, `scenes`, `graphQuery`
- typed template keys/builders such as `sceneById`, `scenePublish`, `graphProjection`

- [ ] **Step 4: Run type-check to verify red before generation**

Run: `cd frontend && npm run type-check`

Expected: FAIL because `src/types/openapi.d.ts` does not exist yet.

- [ ] **Step 5: Generate the OpenAPI snapshot and verify green**

Start backend if needed, then run: `cd frontend && npm run generate:openapi`

Expected: `src/types/openapi.d.ts` is created with exported `paths`.

- [ ] **Step 6: Run targeted checks**

Run:
- `cd frontend && npm test -- src/api/contracts.test.js`
- `cd frontend && npm run type-check`

Expected: both PASS.

## Task 3: Migrate core frontend callers to the contract layer

**Files:**

- Modify: `frontend/src/store/authStore.js`
- Modify: `frontend/src/pages/HomePage.jsx`
- Modify: `frontend/src/pages/PublishCenterPage.jsx`
- Modify: `frontend/src/pages/KnowledgePackageWorkbenchPage.jsx`
- Modify: `frontend/src/pages/datamap-adapter.js`
- Modify: `docs/architecture/system-design.md`
- Modify: `docs/architecture/frontend-workbench-design.md`

- [ ] **Step 1: Replace handwritten paths in core flows**

Import the new contract helpers and replace the highest-value runtime paths first:
- auth/login
- scene list / scene detail / publish
- graphrag query / projection
- datamap graph / node detail / impact analysis

- [ ] **Step 2: Sync the docs**

Add one implementation constraint to the architecture docs:
- backend `/v3/api-docs` is the contract source
- frontend API path constants must come from generated OpenAPI contract keys rather than ad-hoc handwritten endpoint strings

- [ ] **Step 3: Run full frontend verification**

Run: `cd frontend && npm run quality`

Expected: PASS.

- [ ] **Step 4: Run backend verification**

Run:
- `cd backend && mvn -q clean test`
- `curl -I -s http://127.0.0.1:8080/v3/api-docs | sed -n '1,8p'`

Expected:
- backend tests PASS
- OpenAPI endpoint returns `HTTP 200`

- [ ] **Step 5: Run Claude Code review**

Run `./claude-1 -p ...` against the new modified scope and require either concrete findings or the exact conclusion `无异议`.
