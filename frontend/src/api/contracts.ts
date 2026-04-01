import type { paths } from "../types/openapi";

type OpenApiPath = keyof paths & string;
type ClientPath<T extends string> = T extends `/api${infer Rest}` ? Rest : never;
type PathParamValue = string | number;

interface ContractPathParams {
  domainById: { id: PathParamValue };
  sceneById: { id: PathParamValue };
  scenePublish: { id: PathParamValue };
  sceneDiscard: { id: PathParamValue };
  sceneVersions: { id: PathParamValue };
  minimumUnitCheck: { id: PathParamValue };
  graphProjection: { sceneId: PathParamValue };
  graphRebuild: { sceneId: PathParamValue };
  publishChecks: { sceneId: PathParamValue };
  assetsLineage: { sceneId: PathParamValue };
  importTaskById: { taskId: PathParamValue };
  importTaskScenes: { taskId: PathParamValue };
  importTaskQualityConfirm: { taskId: PathParamValue };
  importTaskCompareConfirm: { taskId: PathParamValue };
  importTaskComplete: { taskId: PathParamValue };
  importTaskRewind: { taskId: PathParamValue; step: PathParamValue };
  importTaskCandidateConfirm: { candidateCode: PathParamValue };
  importTaskCandidateGraph: { taskId: PathParamValue };
  importTaskCandidateGraphReview: { taskId: PathParamValue };
  datamapNodeDetail: { id: PathParamValue };
  serviceSpecExport: { sceneId: PathParamValue };
}

function toClientPath<TPath extends OpenApiPath>(path: TPath): ClientPath<TPath> {
  return path.replace(/^\/api/, "") as ClientPath<TPath>;
}

function fillTemplatedPath(template: string, params: Record<string, PathParamValue>) {
  return Object.entries(params).reduce(
    (current, [key, value]) => current.replace(new RegExp(`\\{${key}\\}`, "g"), encodeURIComponent(String(value))),
    template,
  );
}

const STATIC_OPENAPI_PATHS = {
  authToken: "/api/system/auth/token",
  domains: "/api/domains",
  scenes: "/api/scenes",
  serviceSpecs: "/api/service-specs",
  plans: "/api/plans",
  coverageDeclarations: "/api/coverage-declarations",
  policies: "/api/policies",
  contractViews: "/api/contract-views",
  sourceContracts: "/api/source-contracts",
  outputContracts: "/api/output-contracts",
  inputSlotSchemas: "/api/input-slot-schemas",
  graphQuery: "/api/graphrag/query",
  sceneSearch: "/api/scene-search",
  planSelect: "/api/plan-select",
  importPreprocess: "/api/import/preprocess",
  importPreprocessStream: "/api/import/preprocess-stream",
  importTasks: "/api/import/tasks",
  minimumUnitDefinitions: "/api/scenes/minimum-unit",
  datamapGraph: "/api/datamap/graph",
  datamapImpactAnalysis: "/api/datamap/impact-analysis",
  assetsColumns: "/api/assets/columns",
  llmConfig: "/api/system/llm-preprocess-config",
  llmPromptConfig: "/api/system/llm-preprocess-config/prompts",
  llmPromptReset: "/api/system/llm-preprocess-config/prompts/reset",
  llmPromptPreview: "/api/system/llm-preprocess-config/prompts/preview",
  llmConfigModels: "/api/system/llm-preprocess-config/models",
  llmConfigTest: "/api/system/llm-preprocess-config/test",
} as const satisfies Record<string, OpenApiPath>;

const TEMPLATED_OPENAPI_PATHS = {
  domainById: "/api/domains/{id}",
  sceneById: "/api/scenes/{id}",
  scenePublish: "/api/scenes/{id}/publish",
  sceneDiscard: "/api/scenes/{id}/discard",
  sceneVersions: "/api/scenes/{id}/versions",
  minimumUnitCheck: "/api/scenes/{id}/minimum-unit-check",
  graphProjection: "/api/graphrag/projection/{sceneId}",
  graphRebuild: "/api/graphrag/rebuild/{sceneId}",
  publishChecks: "/api/publish-checks/{sceneId}",
  assetsLineage: "/api/assets/lineage/{sceneId}",
  importTaskById: "/api/import/tasks/{taskId}",
  importTaskScenes: "/api/import/tasks/{taskId}/scenes",
  importTaskQualityConfirm: "/api/import/tasks/{taskId}/quality-confirm",
  importTaskCompareConfirm: "/api/import/tasks/{taskId}/compare-confirm",
  importTaskComplete: "/api/import/tasks/{taskId}/complete",
  importTaskRewind: "/api/import/tasks/{taskId}/rewind/{step}",
  importTaskCandidateConfirm: "/api/import/candidates/{candidateCode}/confirm",
  importTaskCandidateGraph: "/api/import/tasks/{taskId}/candidate-graph",
  importTaskCandidateGraphReview: "/api/import/tasks/{taskId}/candidate-graph/review",
  datamapNodeDetail: "/api/datamap/node/{id}/detail",
  serviceSpecExport: "/api/service-specs/export/{sceneId}",
} as const satisfies Record<keyof ContractPathParams, OpenApiPath>;

export const API_CONTRACTS = Object.freeze(
  Object.fromEntries(
    Object.entries(STATIC_OPENAPI_PATHS).map(([key, value]) => [key, toClientPath(value)]),
  ) as { [TKey in keyof typeof STATIC_OPENAPI_PATHS]: ClientPath<(typeof STATIC_OPENAPI_PATHS)[TKey]> },
);

export type ContractPathKey = keyof typeof TEMPLATED_OPENAPI_PATHS;

export function buildApiPath<TKey extends ContractPathKey>(
  key: TKey,
  params: ContractPathParams[TKey],
): ClientPath<(typeof TEMPLATED_OPENAPI_PATHS)[TKey]> {
  const template = TEMPLATED_OPENAPI_PATHS[key];
  return fillTemplatedPath(template, params).replace(/^\/api/, "") as ClientPath<(typeof TEMPLATED_OPENAPI_PATHS)[TKey]>;
}
