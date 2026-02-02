// 领域
export interface Domain {
    id: string
    domainKey: string
    name: string
    description?: string
    sceneCount?: number
    createdAt?: string
    updatedAt?: string
}

// 场景
export interface Scene {
    id: string
    sceneCode: string
    title: string
    domainId: string
    domainKey: string
    domainName: string
    lifecycleStatus: 'ACTIVE' | 'DEPRECATED'

    currentVersionId?: string
    currentVersionLabel?: string
    versionStatus?: 'DRAFT' | 'PUBLISHED'
    hasSensitive: boolean
    lastVerifiedAt?: string
    verifiedBy?: string

    ownerUser: string
    tags: string[]

    createdAt: string
    createdBy: string
    updatedAt: string
    updatedBy: string
}

// 场景版本
export interface SceneVersion {
    id: string
    sceneId: string
    sceneCode: string
    status: 'DRAFT' | 'PUBLISHED'
    isCurrent: boolean

    versionSeq: number
    versionLabel: string

    title: string
    tags: string[]
    ownerUser: string
    contributors: string[]

    hasSensitive: boolean
    lastVerifiedAt?: string
    verifiedBy?: string
    verifyEvidence?: string
    changeSummary?: string

    publishedAt?: string
    publishedBy?: string

    content: SceneVersionContent
    lintResult?: LintResult

    createdAt: string
    createdBy: string
    updatedAt: string
    updatedBy: string
}

// 场景内容
export interface SceneVersionContent {
    sceneDescription: string
    caliberDefinition: string
    inputParams: InputParam[]
    constraintsDescription?: string
    outputSummary?: string
    sqlBlocks: SqlBlock[]
    sourceTables: SourceTable[]
    sensitiveFields: SensitiveField[]
    caveats: Caveat[]
}

// 输入参数
export interface InputParam {
    name: string
    displayName: string
    type: string
    required: boolean
    example?: string
    description?: string
}

// SQL 块
export interface SqlBlock {
    blockId: string
    name: string
    condition?: string
    sql: string
    notes?: string
    extractedTables?: string[]
}

// 数据来源表
export interface SourceTable {
    tableFullname: string
    metadataTableId?: string
    matchStatus: 'MATCHED' | 'NOT_FOUND' | 'BLACKLISTED' | 'VERIFY_FAILED'
    isKey: boolean
    usageType?: string
    partitionField?: string
    source: 'EXTRACTED' | 'MANUAL'
    description?: string
}

// 敏感字段
export interface SensitiveField {
    fieldFullname: string
    tableName: string
    fieldName: string
    sensitivityLevel: string
    maskRule: string
    remarks?: string
}

// 注意事项
export interface Caveat {
    id: string
    title: string
    risk?: string
    text: string
}

// Lint 结果
export interface LintResult {
    passed: boolean
    errorCount: number
    warningCount: number
    errors: LintIssue[]
    warnings: LintIssue[]
}

export interface LintIssue {
    id: string
    message: string
    path: string
    blockId?: string
}

// 列表查询参数
export interface SceneListQuery {
    domainId?: string
    status?: string
    overdue?: boolean
    hasSensitive?: boolean
    keyword?: string
    page?: number
    pageSize?: number
}
