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

  it("keeps the canonical path snapshot stable", () => {
    expect({
      staticPaths: {
        authToken: API_CONTRACTS.authToken,
        scenes: API_CONTRACTS.scenes,
        graphQuery: API_CONTRACTS.graphQuery,
        llmConfigTest: API_CONTRACTS.llmConfigTest,
      },
      templatedPaths: {
        sceneById: buildApiPath("sceneById", { id: 12 }),
        sceneDiscard: buildApiPath("sceneDiscard", { id: 12 }),
        scenePublish: buildApiPath("scenePublish", { id: "12" }),
        graphProjection: buildApiPath("graphProjection", { sceneId: 8 }),
        importTaskRewind: buildApiPath("importTaskRewind", { taskId: "TASK-1", step: 2 }),
      },
    }).toMatchInlineSnapshot(`
      {
        "staticPaths": {
          "authToken": "/system/auth/token",
          "graphQuery": "/graphrag/query",
          "llmConfigTest": "/system/llm-preprocess-config/test",
          "scenes": "/scenes",
        },
        "templatedPaths": {
          "graphProjection": "/graphrag/projection/8",
          "importTaskRewind": "/import/tasks/TASK-1/rewind/2",
          "sceneById": "/scenes/12",
          "sceneDiscard": "/scenes/12/discard",
          "scenePublish": "/scenes/12/publish",
        },
      }
    `);
  });
});
