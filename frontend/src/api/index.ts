import apiClient, { ApiResponse } from './client'
import type { Scene, SceneVersion, SceneListQuery, Domain } from '../types'
import {
    USE_MOCK,
    MOCK_DOMAINS,
    MOCK_SCENES,
    MOCK_DRAFT,
    MOCK_TABLE_SEARCH,
    MOCK_TABLE_DETAIL,
    createMockLintResult,
    delay
} from './mock'

// 场景 API
export const sceneApi = {
    // 获取场景列表
    list: async (params?: SceneListQuery) => {
        if (USE_MOCK) {
            await delay(300)
            let filteredScenes = [...MOCK_SCENES]
            if (params?.domainId) {
                filteredScenes = filteredScenes.filter(s => s.domainId === params.domainId)
            }
            if (params?.keyword) {
                const kw = params.keyword.toLowerCase()
                filteredScenes = filteredScenes.filter(s =>
                    s.title.toLowerCase().includes(kw) ||
                    s.sceneCode.toLowerCase().includes(kw)
                )
            }
            return { data: { code: '0', message: 'success', data: filteredScenes } }
        }
        return apiClient.get<ApiResponse<Scene[]>>('/scenes', { params })
    },

    // 获取场景详情
    get: async (sceneCode: string) => {
        if (USE_MOCK) {
            await delay(200)
            const scene = MOCK_SCENES.find(s => s.sceneCode === sceneCode)
            if (scene) {
                return { data: { code: '0', message: 'success', data: scene } }
            }
            throw new Error('Scene not found')
        }
        return apiClient.get<ApiResponse<Scene>>(`/scenes/${sceneCode}`)
    },

    // 创建场景
    create: async (data: {
        title: string
        domainId?: string
        ownerUser?: string
        tags?: string[]
    }) => {
        if (USE_MOCK) {
            await delay(500)
            const domainId = data.domainId || MOCK_DOMAINS[0].id
            const newScene: Scene = {
                id: `s-${Date.now()}`,
                sceneCode: `SC-NEW-${Date.now().toString().slice(-6)}`,
                title: data.title,
                domainId: domainId,
                domainKey: MOCK_DOMAINS.find(d => d.id === domainId)?.domainKey || 'UNCATEGORIZED',
                domainName: MOCK_DOMAINS.find(d => d.id === domainId)?.name || '未归类',
                lifecycleStatus: 'ACTIVE',
                hasSensitive: false,
                ownerUser: data.ownerUser || 'unknown',
                tags: data.tags || [],
                createdAt: new Date().toISOString(),
                createdBy: data.ownerUser || 'unknown',
                updatedAt: new Date().toISOString(),
                updatedBy: data.ownerUser || 'unknown',
            }
            MOCK_SCENES.unshift(newScene)
            return { data: { code: '0', message: 'success', data: newScene } }
        }
        return apiClient.post<ApiResponse<Scene>>('/scenes', data)
    },

    // 获取草稿
    getDraft: async (sceneCode: string) => {
        if (USE_MOCK) {
            await delay(300)
            // 返回对应场景的草稿
            const draft = { ...MOCK_DRAFT, sceneCode }
            return { data: { code: '0', message: 'success', data: draft } }
        }
        return apiClient.get<ApiResponse<SceneVersion>>(`/scenes/${sceneCode}/draft`)
    },

    // 保存草稿
    saveDraft: async (sceneCode: string, data: Partial<SceneVersion>) => {
        if (USE_MOCK) {
            await delay(500)
            console.log('[Mock] Saving draft for', sceneCode, data)
            return { data: { code: '0', message: 'success', data: { ...MOCK_DRAFT, ...data, sceneCode } } }
        }
        return apiClient.put<ApiResponse<SceneVersion>>(`/scenes/${sceneCode}/draft`, data)
    },

    // 运行 Lint
    lint: async (sceneCode: string) => {
        if (USE_MOCK) {
            await delay(800)
            // 随机返回通过或不通过
            const hasErrors = Math.random() > 0.7
            return { data: { code: '0', message: 'success', data: createMockLintResult(hasErrors) } }
        }
        return apiClient.post<ApiResponse<LintResult>>(`/scenes/${sceneCode}/lint`)
    },

    // 发布
    publish: async (sceneCode: string, data: {
        lastVerifiedAt: string
        verifiedBy: string
        verifyEvidence?: string
        changeSummary: string
    }) => {
        if (USE_MOCK) {
            await delay(1000)
            console.log('[Mock] Publishing', sceneCode, data)
            const published: SceneVersion = {
                ...MOCK_DRAFT,
                sceneCode,
                status: 'PUBLISHED',
                versionSeq: 1,
                versionLabel: 'v1.0',
                lastVerifiedAt: data.lastVerifiedAt,
                verifiedBy: data.verifiedBy,
                verifyEvidence: data.verifyEvidence,
                changeSummary: data.changeSummary,
                publishedAt: new Date().toISOString(),
                publishedBy: 'demo_user',
            }
            return { data: { code: '0', message: 'success', data: published } }
        }
        return apiClient.post<ApiResponse<SceneVersion>>(`/scenes/${sceneCode}/publish`, data)
    },

    // 废弃
    deprecate: async (sceneCode: string, reason?: string) => {
        if (USE_MOCK) {
            await delay(500)
            console.log('[Mock] Deprecating', sceneCode, reason)
            return { data: { code: '0', message: 'success' } }
        }
        return apiClient.post<ApiResponse<void>>(`/scenes/${sceneCode}/deprecate`, { reason })
    },

    // 获取版本列表
    getVersions: async (sceneCode: string) => {
        if (USE_MOCK) {
            await delay(300)
            const versions: SceneVersion[] = [
                { ...MOCK_DRAFT, sceneCode, status: 'PUBLISHED', versionSeq: 1, versionLabel: 'v1.0' },
            ]
            return { data: { code: '0', message: 'success', data: versions } }
        }
        return apiClient.get<ApiResponse<SceneVersion[]>>(`/scenes/${sceneCode}/versions`)
    },

    // 创建新版本
    createVersion: async (sceneCode: string, baseVersion: string = 'PUBLISHED') => {
        if (USE_MOCK) {
            await delay(500)
            const draft = { ...MOCK_DRAFT, sceneCode, versionLabel: 'v1.X-draft' }
            return { data: { code: '0', message: 'success', data: draft } }
        }
        return apiClient.post<ApiResponse<SceneVersion>>(`/scenes/${sceneCode}/versions`, { base: baseVersion })
    },

    // 获取指定版本
    getVersion: async (sceneCode: string, versionId: string) => {
        if (USE_MOCK) {
            await delay(300)
            return { data: { code: '0', message: 'success', data: { ...MOCK_DRAFT, sceneCode, id: versionId } } }
        }
        return apiClient.get<ApiResponse<SceneVersion>>(`/scenes/${sceneCode}/versions/${versionId}`)
    },
}

// 导出 API
export const exportApi = {
    // 下载 doc.json
    downloadDoc: (sceneCode: string) =>
        apiClient.get<Blob>(`/scenes/${sceneCode}/export/doc`, { responseType: 'blob' }),

    // 下载 chunks.json
    downloadChunks: (sceneCode: string) =>
        apiClient.get<Blob>(`/scenes/${sceneCode}/export/chunks`, { responseType: 'blob' }),
}

// 领域 API
export const domainApi = {
    list: async () => {
        if (USE_MOCK) {
            await delay(200)
            return { data: { code: '0', message: 'success', data: MOCK_DOMAINS } }
        }
        return apiClient.get<ApiResponse<Domain[]>>('/domains')
    },

    create: async (data: Partial<Domain>) => {
        if (USE_MOCK) {
            await delay(500)
            const newDomain = {
                id: String(Date.now()),
                domainKey: data.domainKey || 'unknown',
                name: data.name || 'Unknown',
                description: data.description || '',
                sceneCount: 0,
                createdAt: new Date().toISOString().split('T')[0],
                updatedAt: new Date().toISOString().split('T')[0],
            }
            MOCK_DOMAINS.push(newDomain)
            return { data: { code: '0', message: 'success', data: newDomain } }
        }
        return apiClient.post<ApiResponse<Domain>>('/domains', data)
    },

    update: async (id: string, data: Partial<Domain>) => {
        if (USE_MOCK) {
            await delay(500)
            const idx = MOCK_DOMAINS.findIndex(d => d.id === id)
            if (idx > -1) {
                MOCK_DOMAINS[idx] = { ...MOCK_DOMAINS[idx], ...data, updatedAt: new Date().toISOString().split('T')[0] }
                return { data: { code: '0', message: 'success', data: MOCK_DOMAINS[idx] } }
            }
            throw new Error('Domain not found')
        }
        return apiClient.put<ApiResponse<Domain>>(`/domains/${id}`, data)
    },

    delete: async (id: string) => {
        if (USE_MOCK) {
            await delay(500)
            const idx = MOCK_DOMAINS.findIndex(d => d.id === id)
            if (idx > -1) {
                MOCK_DOMAINS.splice(idx, 1)
                return { data: { code: '0', message: 'success' } }
            }
            return { data: { code: '0', message: 'success' } }
        }
        return apiClient.delete<ApiResponse<void>>(`/domains/${id}`)
    }
}

// 元数据 API
export const metadataApi = {
    searchTables: async (keyword: string) => {
        if (USE_MOCK) {
            await delay(400)
            const kw = keyword.toLowerCase()
            const results = MOCK_TABLE_SEARCH.filter(t =>
                t.tableFullname.toLowerCase().includes(kw) ||
                t.description.toLowerCase().includes(kw)
            )
            return { data: { code: '0', message: 'success', data: results } }
        }
        return apiClient.get<ApiResponse<TableSearchResult[]>>('/metadata/tables/search', {
            params: { keyword },
        })
    },

    getTableDetail: async (tableFullname: string) => {
        if (USE_MOCK) {
            await delay(300)
            // 返回mock详情，调整表名
            return { data: { code: '0', message: 'success', data: { ...MOCK_TABLE_DETAIL, tableFullname } } }
        }
        return apiClient.get<ApiResponse<TableDetail>>(`/metadata/tables/${tableFullname}`)
    },
}

// 设置 API
export const settingsApi = {
    // 测试元数据连接
    testMetadata: async () => {
        if (USE_MOCK) {
            await delay(1000)
            return { data: { code: '0', message: 'success', data: 'Connection successful' } }
        }
        return apiClient.post<ApiResponse<string>>('/settings/metadata/test')
    },

    getLintSettings: async () => {
        if (USE_MOCK) {
            await delay(300)
            return {
                data: {
                    code: '0', message: 'success', data: {
                        rules: [
                            { id: 'require_desc', name: 'Require Description', enabled: true, severity: 'error' },
                            { id: 'no_select_star', name: 'No SELECT *', enabled: true, severity: 'warning' },
                            { id: 'check_sensitive', name: 'Check Sensitive Fields', enabled: true, severity: 'error' },
                            { id: 'sql_max_lines', name: 'Max SQL Lines', enabled: false, severity: 'warning', value: 100 },
                        ]
                    }
                }
            }
        }
        return apiClient.get<ApiResponse<any>>('/settings/lint')
    },

    saveLintSettings: async (settings: any) => {
        if (USE_MOCK) {
            await delay(500)
            return { data: { code: '0', message: 'success' } }
        }
        return apiClient.put<ApiResponse<void>>('/settings/lint', settings)
    }
}

// 类型定义
export interface LintResult {
    passed: boolean
    errorCount?: number
    warningCount?: number
    errors: LintIssue[]
    warnings: LintIssue[]
}

export interface LintIssue {
    id: string
    message: string
    path: string
    blockId?: string
}

export interface TableSearchResult {
    tableFullname: string
    metadataTableId: string
    description: string
    hasSensitiveFields: boolean
}

export interface TableDetail {
    tableFullname: string
    metadataTableId: string
    schemaName: string
    tableName: string
    description: string
    sensitivitySummary?: string
    fields: FieldInfo[]
}

export interface FieldInfo {
    fieldName: string
    fieldFullname: string
    metadataFieldId: string
    dataType: string
    description: string
    sensitivityLevel?: string
    isSensitive: boolean
}

// 导入格式 API
export interface ImportFormatSchema {
    schema_id: string
    schema_version: string
    $schema: string
    title: string
    type: string
    required: string[]
    properties: Record<string, unknown>
    $defs: Record<string, unknown>
    _meta?: {
        generated_at: string
        lang: string
    }
}

export interface ImportFormatTemplate {
    doc_type: string
    schema_version: string
    source_type: string
    global: Record<string, unknown>
    scenes: Array<Record<string, unknown>>
    parse_report: Record<string, unknown>
    _meta?: {
        schema_id: string
        schema_version: string
        mode: string
        generated_at: string
        lang: string
    }
}

export interface ImportFormatVersions {
    current: string
    supported: string[]
    schemas: Array<{
        version: string
        schema_id: string
        schema_version: string
        doc_type: string
    }>
}

export const importFormatApi = {
    // 获取导入 Schema
    getSchema: async (version = 'v1', lang = 'zh') => {
        return apiClient.get<ImportFormatSchema>('/import-format/schema', {
            params: { version, lang }
        })
    },

    // 获取导入模板
    getTemplate: async (version = 'v1', mode = 'empty', lang = 'zh') => {
        return apiClient.get<ImportFormatTemplate>('/import-format/template', {
            params: { version, mode, lang }
        })
    },

    // 获取支持的版本列表
    getVersions: async () => {
        return apiClient.get<ImportFormatVersions>('/import-format/versions')
    }
}
