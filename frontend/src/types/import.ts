/**
 * 文档导入相关类型定义
 */

export type ImportSourceType = "PASTE_MD" | "FILE_MD" | "FILE_TXT";
export type ImportMode = "split_by_h2" | "single_scene";

export interface ImportParseRequest {
    sourceType: ImportSourceType;
    rawText: string;
    mode: ImportMode;
}

export interface ImportCommitRequest {
    sourceType: ImportSourceType;
    rawText: string;
    mode: ImportMode;
    selectedTempIds: string[];
    defaultDomainId?: string;
}

/** draftContent：写入 scene_version.content_json 的结构 */
export interface SceneDraftContent {
    sceneTitle?: string;
    title?: string;
    domainId?: string;
    tags?: string[];
    ownerUser?: string;
    contributors?: string[];

    sceneDescription?: string;
    caliberDefinition?: string;
    applicability?: string;
    boundaries?: string;
    entities?: string[];

    inputs?: {
        params: InputParam[];
        constraints?: InputConstraint[];
    };

    outputs?: {
        summary: string;
        fields?: string[];
    };

    sqlBlocks?: SqlBlockDto[];
    caveats?: CaveatDto[];
    sourceTablesHint?: SourceTableHint[];
    sensitiveFieldsHint?: SensitiveFieldHint[];
}

export interface SqlBlockDto {
    blockId?: string;
    name: string;
    condition?: string;
    sql: string;
    notes?: string;
}

export interface InputParam {
    name: string;
    nameEn?: string;
    nameZh?: string;
    type?: "STRING" | "INT" | "DATE" | "ENUM" | "DECIMAL" | "UNKNOWN";
    required?: boolean;
    example?: string;
    description?: string;
}

export interface InputConstraint {
    name: string;
    description: string;
    required: boolean;
    impact?: string;
}

export interface CaveatDto {
    id?: string;
    title?: string;
    text: string;
    level: "LOW" | "MEDIUM" | "HIGH";
    risk?: "LOW" | "MEDIUM" | "HIGH";
}

export interface SourceTableHint {
    table: string;
    confidence: number;
    description: string;
}

export interface SensitiveFieldHint {
    field: string;
    confidence: number;
    description: string;
}

export interface ParseStats {
    sqlBlocks: number;
    tablesExtracted: number;
}

export interface SceneCandidate {
    tempId: string;
    titleGuess: string;
    draftContent: SceneDraftContent;
    parseStats: ParseStats;
    warnings: string[];
    errors: string[];
}

export interface ParseReportScene {
    tempId: string;
    titleGuess: string;
    confidence: number;
    fieldsMapped: Record<
        "sceneDescription" | "caliberDefinition" | "contributors" | "caveats" | "sqlBlocks",
        "HIGH" | "MEDIUM" | "LOW" | "NONE"
    >;
    sqlBlocksFound: number;
    warnings: string[];
    errors: string[];
}

export interface ImportParseResponse {
    mode: ImportMode;
    sceneCandidates: SceneCandidate[];
    parseReport: {
        parser: string;
        mode: ImportMode;
        global_warnings: string[];
        global_errors: string[];
        scenes: ParseReportScene[];
    };
}

export interface ImportCommitResponse {
    createdScenes: Array<{
        sceneCode: string;
        draftVersionId: string;
    }>;
}

// ImportDrawer Props
export interface ImportDrawerProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    defaultMode?: ImportMode;
    defaultSourceType?: ImportSourceType;
    maxBytes?: number;
    onImported?: (res: ImportCommitResponse) => void;
    defaultDomainId?: string;
}

// ImportDrawer State
export type ImportStep = 1 | 2;

export type ImportStatus =
    | "IDLE"
    | "INPUT_READY"
    | "PARSING"
    | "PREVIEW"
    | "COMMITTING"
    | "DONE"
    | "ERROR";

export interface ImportDrawerState {
    step: ImportStep;
    status: ImportStatus;
    sourceType: ImportSourceType;
    mode: ImportMode;
    rawText: string;
    rawBytes: number;
    parseResponse?: ImportParseResponse;
    selectedTempIds: string[];
    error?: {
        stage: "parse" | "commit" | "file" | "validation";
        message: string;
        detail?: unknown;
    };
    commitResponse?: ImportCommitResponse;
}
