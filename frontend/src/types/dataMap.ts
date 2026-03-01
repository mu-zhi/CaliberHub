export type NodeType = "ROOT" | "TOPIC" | "SCENE" | "TABLE";

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

export interface LineageGraphData {
  nodes: {
    id: string;
    label: string;
    type: "SOURCE" | "WAREHOUSE" | "MART" | "APP";
    status?: string;
  }[];
  edges: {
    source: string;
    target: string;
    label?: string;
  }[];
  truncated?: boolean;
  hiddenNodeCount?: number;
}
