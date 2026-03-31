export type NodeType = "ROOT" | "TOPIC" | "SCENE" | "TABLE";
export type AssetObjectType =
  | "DOMAIN"
  | "SCENE"
  | "PLAN"
  | "OUTPUT_CONTRACT"
  | "CONTRACT_VIEW"
  | "COVERAGE_DECLARATION"
  | "POLICY"
  | "EVIDENCE_FRAGMENT"
  | "SOURCE_CONTRACT"
  | "SOURCE_INTAKE_CONTRACT"
  | "PATH_TEMPLATE"
  | "VERSION_SNAPSHOT";

export interface MillerNode {
  id: string;
  parentId: string | null;
  label: string;
  type: NodeType;
  hasChildren: boolean;
  status?: "DRAFT" | "PUBLISHED" | "RISK" | "DISCARDED";
  meta?: any;
}

export interface FetchColumnResponse {
  columnId: string;
  items: MillerNode[];
}

export interface DataMapGraphNode {
  id: string;
  label: string;
  type: AssetObjectType | string;
  objectType: AssetObjectType | string;
  objectCode?: string;
  objectName?: string;
  status?: string;
  snapshotId?: number;
  domainCode?: string;
  owner?: string;
  sensitivityScope?: string;
  timeSemantic?: string;
  evidenceCount?: number;
  lastReviewedAt?: string;
  summaryText?: string;
  meta?: any;
}

export interface DataMapGraphEdge {
  id: string;
  source: string;
  target: string;
  label?: string;
  relationType?: string;
  confidence?: number;
  traceId?: string;
  sourceRef?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  policyHit?: boolean;
  coverageExplanation?: string;
  meta?: any;
}

export interface LineageGraphData {
  rootNodeId?: string;
  sceneId?: number;
  sceneName?: string;
  nodes: DataMapGraphNode[];
  edges: DataMapGraphEdge[];
  truncated?: boolean;
  hiddenNodeCount?: number;
}

export interface DataMapNodeDetail {
  assetRef: string;
  node: DataMapGraphNode;
  attributes: Record<string, any>;
}

export interface DataMapImpactAsset {
  assetRef: string;
  objectType: string;
  objectName: string;
  relationType?: string;
  impactSummary?: string;
}

export interface DataMapImpactAnalysis {
  assetRef: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH" | string;
  recommendedActions: string[];
  affectedAssets: DataMapImpactAsset[];
  graph: LineageGraphData | null;
}
