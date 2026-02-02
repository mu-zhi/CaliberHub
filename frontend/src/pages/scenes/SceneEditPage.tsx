import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import {
    Form,
    Input,
    Button,
    Space,
    Tag,
    Typography,
    Alert,
    message,
    Modal,
    DatePicker,
    Table,
    Select,
    Progress,
    Drawer,
    Spin,
    Dropdown,
} from 'antd'
import type { MenuProps } from 'antd'
import {
    SaveOutlined,
    CheckCircleOutlined,
    SendOutlined,
    CloseCircleOutlined,
    PlusOutlined,
    DeleteOutlined,
    ImportOutlined,
    ExportOutlined,
    ArrowLeftOutlined,
    DownloadOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { sceneApi, metadataApi, domainApi, exportApi } from '@/api'
import type {
    SqlBlock,
    SourceTable,
    SensitiveField,
    InputParam,
    Caveat,
    Domain,
    LintResult,
} from '@/types'
import type { SceneDraftContent } from '@/types/import'
import type { TableSearchResult } from '@/api'

import EditorLayout from '@/components/editor/EditorLayout'
import SectionNav, { SectionItem } from '@/components/editor/SectionNav'
import FormCard from '@/components/editor/FormCard'
import RightPanelTabs from '@/components/editor/RightPanelTabs'
import SqlBlockEditor from '@/components/editor/SqlBlockEditor'
import ImportDrawer from '@/components/business/ImportDrawer'

const { Title, Text } = Typography
const { TextArea } = Input

// 匹配状态配置
const MATCH_STATUS_CONFIG: Record<string, { color: string; text: string }> = {
    MATCHED: { color: 'success', text: '✅ 已匹配' },
    NOT_FOUND: { color: 'error', text: '❌ 未找到' },
    BLACKLISTED: { color: 'error', text: '🚫 黑名单' },
    VERIFY_FAILED: { color: 'warning', text: '⚠️ 校验失败' },
}

// 脱敏规则选项
const MASK_RULE_OPTIONS = [
    { value: 'FULL_MASK', label: '全掩码' },
    { value: 'PARTIAL_MASK', label: '保留后四位' },
    { value: 'HASH', label: '哈希' },
    { value: 'HIDE', label: '不展示' },
]

function SceneEditorPage() {
    const { id } = useParams<{ id: string }>()
    const location = useLocation()
    const navigate = useNavigate()
    const [form] = Form.useForm()
    const [publishForm] = Form.useForm()

    // 状态
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [linting, setLinting] = useState(false)
    const [publishing, setPublishing] = useState(false)
    const [publishModalVisible, setPublishModalVisible] = useState(false)
    const [metadataDrawerVisible, setMetadataDrawerVisible] = useState(false)
    const [activeSection, setActiveSection] = useState('basic')

    // 数据
    const [sceneCode, setSceneCode] = useState<string>('')
    const [domains, setDomains] = useState<Domain[]>([])
    const [lintResult, setLintResult] = useState<LintResult | null>(null)
    const [sourceTables, setSourceTables] = useState<SourceTable[]>([])
    const [sensitiveFields, setSensitiveFields] = useState<SensitiveField[]>([])
    const [sqlBlocks, setSqlBlocks] = useState<SqlBlock[]>([{ blockId: 'blk-001', name: '主查询', sql: '' }])
    const [inputParams, setInputParams] = useState<InputParam[]>([])
    const [caveats, setCaveats] = useState<Caveat[]>([])

    // 元数据搜索
    const [tableSearchKeyword, setTableSearchKeyword] = useState('')
    const [tableSearchResults, setTableSearchResults] = useState<TableSearchResult[]>([])
    const [searchingTables, setSearchingTables] = useState(false)

    // 导入相关
    const [importDrawerVisible, setImportDrawerVisible] = useState(false)

    // 导出逻辑
    const handleExport = async (type: 'doc' | 'chunks') => {
        if (!sceneCode) return
        try {
            const apiCall = type === 'doc' ? exportApi.downloadDoc : exportApi.downloadChunks
            const res = await apiCall(sceneCode)

            const blob = new Blob([res.data], { type: 'application/json' })
            const url = window.URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            const disposition = res.headers['content-disposition']
            let filename = `${sceneCode}_${type}.json`
            if (disposition && disposition.indexOf('attachment') !== -1) {
                const filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                const matches = filenameRegex.exec(disposition);
                if (matches != null && matches[1]) {
                    filename = matches[1].replace(/['"]/g, '');
                }
            }
            a.download = filename
            a.click()
            window.URL.revokeObjectURL(url)
            message.success('导出成功')
        } catch (e) {
            console.error(e)
            message.error('导出失败')
        }
    }

    const exportMenuProps: MenuProps = {
        items: [
            {
                key: 'doc',
                label: '导出标准文档 (doc.json)',
                icon: <DownloadOutlined />,
                onClick: () => handleExport('doc'),
            },
            {
                key: 'chunks',
                label: '导出 RAG 切片 (chunks.json)',
                icon: <DownloadOutlined />,
                onClick: () => handleExport('chunks'),
            },
        ],
    }

    // 加载初始数据
    useEffect(() => {
        const loadData = async () => {
            try {
                // 加载领域列表
                const domainRes = await domainApi.list()
                if (domainRes.data.data) {
                    setDomains(domainRes.data.data)
                }

                // 如果是编辑已有场景
                if (id && id !== 'new') {
                    const draftRes = await sceneApi.getDraft(id)
                    if (draftRes.data.data) {
                        const draft = draftRes.data.data
                        setSceneCode(draft.sceneCode)
                        form.setFieldsValue({
                            title: draft.title,
                            domainId: draft.content?.sceneDescription ? undefined : undefined, // Simplify logic if needed
                            tags: draft.tags,
                            ownerUser: draft.ownerUser,
                            sceneDescription: draft.content?.sceneDescription,
                            caliberDefinition: draft.content?.caliberDefinition,
                            outputSummary: draft.content?.outputSummary,
                        })
                        // Handle domainId logic separately if complex, assuming draft has domainId at top level or content level?
                        // Original code: domainId: draft.content?.sceneDescription ? undefined : undefined
                        // That looked weird. Assuming draft has domainId.
                        // Let's check type. SceneDraft doesn't strictly adhere to checks here, using form's initialValues is safer.
                        // Correcting:
                        // form.setFieldsValue({ ...draft, ...draft.content }) - caution with overwrite
                        form.setFieldsValue({
                            title: draft.title,
                            domainId: (draft as any).domainId, // Cast if type mismatch
                            ownerUser: draft.ownerUser,
                            tags: draft.tags,
                            sceneDescription: draft.content?.sceneDescription,
                            caliberDefinition: draft.content?.caliberDefinition,
                            outputSummary: draft.content?.outputSummary,
                        })

                        setSqlBlocks(draft.content?.sqlBlocks || [])
                        setSourceTables(draft.content?.sourceTables || [])
                        setSensitiveFields(draft.content?.sensitiveFields || [])
                        setInputParams(draft.content?.inputParams || [])
                        setCaveats(draft.content?.caveats || [])
                    }
                }
            } catch (error) {
                console.error('加载数据失败:', error)
                message.error('加载数据失败')
            } finally {
                setLoading(false)
            }
        }
        loadData()
    }, [id, form])

    // 如果从导入跳转，带上草稿内容
    useEffect(() => {
        const importedDraft = (location.state as any)?.importedDraft as SceneDraftContent | undefined
        if (importedDraft) {
            applyImportedContent(importedDraft)
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.state])

    // 计算完成度
    const calculateCompletion = useCallback(() => {
        const formValues = form.getFieldsValue()
        const checks = [
            { id: 'basic', name: '基本信息', done: !!formValues.title },
            { id: 'definition', name: '口径定义', done: !!formValues.caliberDefinition },
            { id: 'inputs', name: '输入参数', done: inputParams.length > 0 },
            { id: 'outputs', name: '输出字段', done: !!formValues.outputSummary },
            { id: 'tables', name: '数据来源表', done: sourceTables.length > 0 },
            { id: 'sensitive', name: '敏感字段', done: sourceTables.some(t => t.matchStatus === 'MATCHED') ? sensitiveFields.length > 0 : true },
            { id: 'sql', name: 'SQL方案', done: sqlBlocks.some(b => b.sql?.trim()) },
            { id: 'caveats', name: '注意事项', done: caveats.length > 0 },
        ]
        const completed = checks.filter(c => c.done).length
        return { checks, completed, total: checks.length, percent: Math.round((completed / checks.length) * 100) }
    }, [form, inputParams, sourceTables, sensitiveFields, sqlBlocks, caveats])

    const [completion, setCompletion] = useState(calculateCompletion())

    useEffect(() => {
        const timer = setTimeout(() => {
            setCompletion(calculateCompletion())
        }, 100)
        return () => clearTimeout(timer)
    }, [calculateCompletion, form])
    // form changes won't trigger this effect directly, need onValuesChange.
    // But completion depends on state (inputParams etc) which changes commonly.

    // 保存草稿
    const handleSave = async () => {
        setSaving(true)
        try {
            const values = await form.validateFields()

            if (!sceneCode) {
                // 新建场景
                const createRes = await sceneApi.create({
                    title: values.title,
                    domainId: values.domainId,
                    ownerUser: values.ownerUser || 'demo_user',
                    tags: values.tags || [],
                })
                if (createRes.data.data) {
                    setSceneCode(createRes.data.data.sceneCode)
                    message.success('场景创建成功')
                    navigate(`/scenes/${createRes.data.data.sceneCode}/edit`, { replace: true })
                }
            } else {
                // 保存草稿
                await sceneApi.saveDraft(sceneCode, {
                    title: values.title,
                    tags: values.tags || [],
                    ownerUser: values.ownerUser,
                    content: {
                        sceneDescription: values.sceneDescription || '',
                        caliberDefinition: values.caliberDefinition || '',
                        inputParams,
                        outputSummary: values.outputSummary,
                        sqlBlocks,
                        sourceTables,
                        sensitiveFields,
                        caveats,
                    },
                })
                message.success('保存成功')
            }
        } catch (error: any) {
            if (error?.errorFields) {
                message.error('请检查表单填写')
            } else {
                message.error('保存失败')
            }
        } finally {
            setSaving(false)
        }
    }

    // 运行 Lint
    const handleLint = async () => {
        if (!sceneCode) {
            message.warning('请先保存场景')
            return
        }

        setLinting(true)
        try {
            await handleSave()
            const lintRes = await sceneApi.lint(sceneCode)
            if (lintRes.data.data) {
                setLintResult(lintRes.data.data as unknown as LintResult)
                if (lintRes.data.data.passed) {
                    message.success('校验通过')
                } else {
                    message.warning('校验完成，存在问题')
                }
            }
        } catch {
            message.error('校验失败')
        } finally {
            setLinting(false)
        }
    }

    // 发布
    const handlePublish = async () => {
        if (!sceneCode) {
            message.warning('请先保存场景')
            return
        }

        if (lintResult && !lintResult.passed) {
            message.error('存在校验错误，无法发布')
            return
        }
        setPublishModalVisible(true)
    }

    const handlePublishConfirm = async () => {
        try {
            const values = await publishForm.validateFields()
            setPublishing(true)

            await sceneApi.publish(sceneCode, {
                lastVerifiedAt: dayjs(values.lastVerifiedAt).format('YYYY-MM-DD'),
                verifiedBy: values.verifiedBy,
                verifyEvidence: values.verifyEvidence,
                changeSummary: values.changeSummary,
            })

            message.success('发布成功')
            setPublishModalVisible(false)
            navigate(`/scenes/${sceneCode}`)
        } catch {
            message.error('发布失败')
        } finally {
            setPublishing(false)
        }
    }

    // 应用导入的内容
    const applyImportedContent = (draft: SceneDraftContent) => {
        // 兼容多种字段命名（来自导入 JSON）
        const title = (draft as any).scene_title || draft.title || form.getFieldValue('title')
        const sceneDesc = (draft as any).sceneDescription || (draft as any).scene_description
        const caliber = (draft as any).caliberDefinition || (draft as any).caliber_definition
        const outputSummary = draft.outputs?.summary

        form.setFieldsValue({
            title,
            sceneDescription: sceneDesc,
            caliberDefinition: caliber,
            outputSummary,
        })

        // SQL blocks
        const sqlBlocksInput = (draft as any).sqlBlocks || (draft as any).sql_blocks
        if (sqlBlocksInput) {
            setSqlBlocks(sqlBlocksInput.map((b: any, i: number) => ({
                blockId: `blk-${Date.now()}-${i}`,
                name: b.name || `SQL块${i + 1}`,
                sql: b.sql,
                notes: b.notes,
            })))
        }

        // 输入参数
        const paramsInput = draft.inputs?.params || (draft as any).inputs?.params
        if (paramsInput) {
            setInputParams(paramsInput.map((p: any, idx: number) => ({
                name: p.nameEn || p.name || `p${idx + 1}`,
                displayName: p.nameZh || p.name || '',
                type: p.type || 'STRING',
                required: p.required ?? true,
                description: p.description,
                example: p.example,
            })))
        }

        // 输出摘要
        if ((draft as any).outputs?.summary) {
            form.setFieldsValue({ outputSummary: (draft as any).outputs.summary })
        }

        // 来源表提示
        const tableHints = (draft as any).source_tables_hint
        if (tableHints) {
            setSourceTables(tableHints.map((t: any) => ({
                tableFullname: t.table,
                matchStatus: 'MATCHED',
                isKey: true,
                source: 'MANUAL',
                description: t.description,
            })))
        }

        // 敏感字段提示
        const sensitiveHints = (draft as any).sensitive_fields_hint
        if (sensitiveHints) {
            setSensitiveFields(sensitiveHints.map((f: any, i: number) => ({
                fieldFullname: f.field,
                tableName: f.table || '',
                fieldName: f.field,
                sensitivityLevel: 'SENSITIVE',
                maskRule: 'FULL_MASK',
                remarks: f.description,
            })))
        }

        if (draft.caveats) {
            setCaveats(draft.caveats.map((c, i) => ({
                id: `cav-${Date.now()}-${i}`,
                title: c.title,
                text: c.text,
                risk: c.risk,
            })))
        }

        message.success('已应用导入内容')
    }

    // ... (Keep existing helpers like extractAndImportTables, searchMetadataTables, tables/fields logic)
    // For brevity in this tool call, I'll copy the logic if I can, or re-implement.
    // Since I'm overwriting, I MUST include them.

    // 从SQL抽取表名并导入
    const extractAndImportTables = async () => {
        const extractedNames = new Set<string>()

        for (const block of sqlBlocks) {
            if (!block.sql) continue
            const regex = /(?:FROM|JOIN)\s+([a-zA-Z_][a-zA-Z0-9_.]*)/gi
            let match
            while ((match = regex.exec(block.sql)) !== null) {
                const tableName = match[1].toLowerCase()
                if (!tableName.startsWith('(')) {
                    extractedNames.add(tableName)
                }
            }
        }

        if (extractedNames.size === 0) {
            message.info('未从SQL中抽取到表名')
            return
        }

        const existingNames = new Set(sourceTables.map(t => t.tableFullname.toLowerCase()))
        const newTables: SourceTable[] = []

        for (const name of extractedNames) {
            if (!existingNames.has(name)) {
                newTables.push({
                    tableFullname: name,
                    matchStatus: 'VERIFY_FAILED',
                    isKey: true,
                    source: 'EXTRACTED',
                })
            }
        }

        if (newTables.length > 0) {
            setSourceTables([...sourceTables, ...newTables])
            message.success(`已导入 ${newTables.length} 个表`)
        } else {
            message.info('所有表已存在')
        }
    }

    const searchMetadataTables = async () => {
        if (!tableSearchKeyword.trim()) return
        setSearchingTables(true)
        try {
            const res = await metadataApi.searchTables(tableSearchKeyword)
            if (res.data.data) {
                setTableSearchResults(res.data.data)
            }
        } catch {
            message.error('搜索失败')
        } finally {
            setSearchingTables(false)
        }
    }

    const addTable = (table: TableSearchResult) => {
        const exists = sourceTables.some(t =>
            t.tableFullname.toLowerCase() === table.tableFullname.toLowerCase()
        )
        if (exists) {
            message.warning('该表已存在')
            return
        }

        setSourceTables([...sourceTables, {
            tableFullname: table.tableFullname,
            metadataTableId: table.metadataTableId,
            matchStatus: 'MATCHED',
            isKey: true,
            source: 'MANUAL',
            description: table.description,
        }])
        message.success('已添加表')
    }

    const removeTable = (tableFullname: string) => {
        setSourceTables(sourceTables.filter(t => t.tableFullname !== tableFullname))
    }



    const removeSensitiveField = (fieldFullname: string) => {
        setSensitiveFields(sensitiveFields.filter(f => f.fieldFullname !== fieldFullname))
    }

    const updateMaskRule = (fieldFullname: string, maskRule: string) => {
        setSensitiveFields(sensitiveFields.map(f =>
            f.fieldFullname === fieldFullname ? { ...f, maskRule } : f
        ))
    }

    // Column definitions
    const sourceTableColumns: ColumnsType<SourceTable> = [
        { title: '表名', dataIndex: 'tableFullname', key: 'tableFullname', render: (text) => <Text code copyable>{text}</Text> },
        { title: '状态', dataIndex: 'matchStatus', key: 'matchStatus', render: (s: string) => { const c = MATCH_STATUS_CONFIG[s]; return c ? <Tag color={c.color}>{c.text}</Tag> : s } },
        {
            title: '操作', key: 'action', render: (_, r) => (
                <Space>
                    <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => removeTable(r.tableFullname)} />
                </Space>
            )
        }
    ]

    const sensitiveFieldColumns: ColumnsType<SensitiveField> = [
        { title: '字段', dataIndex: 'fieldFullname', key: 'fieldFullname', render: (text) => <Text code>{text}</Text> },
        { title: '等级', dataIndex: 'sensitivityLevel', key: 'sensitivityLevel' },
        {
            title: '规则', dataIndex: 'maskRule', key: 'maskRule', render: (rule, r) => (
                <Select value={rule} style={{ width: 120 }} options={MASK_RULE_OPTIONS} onChange={(v) => updateMaskRule(r.fieldFullname, v)} />
            )
        },
        { title: '操作', key: 'action', render: (_, r) => <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => removeSensitiveField(r.fieldFullname)} /> }
    ]

    const scrollToSection = (sectionId: string) => {
        setActiveSection(sectionId)
        const el = document.getElementById(`section-${sectionId}`)
        el?.scrollIntoView({ behavior: 'smooth' })
    }

    const sections: SectionItem[] = completion.checks.map(c => ({
        id: c.id,
        title: c.name,
        isCompleted: c.done,
        hasError: !c.done && linting
    }))

    if (loading) return <div className="flex justify-center items-center h-screen"><Spin size="large" /></div>

    // Render Logic
    const leftNav = <SectionNav sections={sections} activeSection={activeSection} onSectionClick={scrollToSection} />

    const rightPanel = (
        <RightPanelTabs
            lintErrorCount={lintResult ? lintResult.errors.length : 0}
            tableCount={sourceTables.length}
            lintContent={
                lintResult ? (
                    <div>
                        {lintResult.passed && <Alert message="Passed" type="success" showIcon />}
                        {lintResult.errors.map((e, i) => (
                            <Alert key={i} message={e.message} type="error" showIcon className="mb-2" />
                        ))}
                        {lintResult.warnings.map((w, i) => (
                            <Alert key={i} message={w.message} type="warning" showIcon className="mb-2" />
                        ))}
                    </div>
                ) : (
                    <EmptyState text="请运行校验查看结果" />
                )
            }
            tablesContent={
                sourceTables.length > 0 ? (
                    <div className="space-y-2">
                        {sourceTables.map(t => (
                            <div key={t.tableFullname} className="flex items-center justify-between text-xs border-b pb-1">
                                <Text ellipsis style={{ maxWidth: 180 }}>{t.tableFullname}</Text>
                                <Tag>{t.matchStatus}</Tag>
                            </div>
                        ))}
                    </div>
                ) : (
                    <EmptyState text="暂无来源表" />
                )
            }
            completionContent={
                <div className="space-y-2">
                    <Progress percent={completion.percent} />
                    {completion.checks.map(c => (
                        <div key={c.id} className="flex items-center text-xs">
                            {c.done ? <CheckCircleOutlined className="text-success mr-2" /> : <CloseCircleOutlined className="text-danger mr-2" />}
                            {c.name}
                        </div>
                    ))}
                </div>
            }
        />
    )

    return (
        <>
            <div className="h-14 border-b border-border bg-card flex items-center px-4 justify-between sticky top-0 z-20">
                <div className="flex items-center gap-3">
                    <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/scenes')} />
                    <Title level={5} style={{ margin: 0 }}>{id === 'new' ? '新建场景' : sceneCode}</Title>
                    <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-muted text-muted-foreground border border-border">草稿</span>
                </div>
                <Space>
                    <Button icon={<SaveOutlined />} onClick={handleSave} loading={saving}>保存</Button>
                    <Button icon={<CheckCircleOutlined />} onClick={handleLint} loading={linting}>校验</Button>
                    <Button icon={<ImportOutlined />} onClick={() => setImportDrawerVisible(true)}>导入</Button>
                    <Dropdown menu={exportMenuProps}>
                        <Button icon={<ExportOutlined />}>导出</Button>
                    </Dropdown>
                    <Button type="primary" icon={<SendOutlined />} onClick={handlePublish} disabled={!lintResult?.passed}>发布</Button>
                </Space>
            </div>

            <EditorLayout leftNav={leftNav} rightPanel={rightPanel}>
                <Form form={form} layout="vertical" onValuesChange={() => setCompletion(calculateCompletion())}>

                    <FormCard id="section-basic" title="1. 基本信息">
                        <Form.Item label="场景名称" name="title" rules={[{ required: true }]}>
                            <Input placeholder="输入场景标题" />
                        </Form.Item>
                        <Form.Item label="所属领域" name="domainId" rules={[{ required: true }]}>
                            <Select placeholder="选择领域" options={domains.map(d => ({ value: d.id, label: d.name }))} />
                        </Form.Item>
                        <Form.Item label="负责人" name="ownerUser">
                            <Input placeholder="负责人用户名" />
                        </Form.Item>
                        <Form.Item label="标签" name="tags">
                            <Select mode="tags" placeholder="输入标签" />
                        </Form.Item>
                    </FormCard>

                    <FormCard id="section-definition" title="2. 口径定义">
                        <Form.Item label="场景描述" name="sceneDescription">
                            <TextArea rows={3} placeholder="简述场景用途" />
                        </Form.Item>
                        <Form.Item label="口径逻辑定义" name="caliberDefinition" rules={[{ required: true }]}>
                            <TextArea rows={4} placeholder="详细描述业务口径逻辑" />
                        </Form.Item>
                    </FormCard>

                    <FormCard id="section-inputs" title="3. 输入参数">
                        <Table
                            dataSource={inputParams}
                            columns={[
                                { title: '参数名', dataIndex: 'name' },
                                { title: '类型', dataIndex: 'type' },
                                { title: '必填', dataIndex: 'required', render: (r) => r ? '是' : '否' }
                            ]}
                            pagination={false}
                            size="small"
                        />
                        <Button type="dashed" block icon={<PlusOutlined />} className="mt-2" onClick={() => setInputParams([...inputParams, { name: `p${inputParams.length}`, displayName: '', type: 'VARCHAR', required: false }])}>
                            添加参数
                        </Button>
                    </FormCard>

                    <FormCard id="section-outputs" title="4. 输出定义">
                        <Form.Item label="输出摘要" name="outputSummary">
                            <TextArea rows={2} placeholder="描述输出内容" />
                        </Form.Item>
                    </FormCard>

                    <FormCard id="section-tables" title="5. 数据来源表" extra={<Button size="small" onClick={extractAndImportTables}>从SQL抽取</Button>}>
                        <div className="mb-2">
                            <Button type="dashed" size="small" icon={<PlusOutlined />} onClick={() => setMetadataDrawerVisible(true)}>添加表</Button>
                        </div>
                        <Table dataSource={sourceTables} columns={sourceTableColumns} pagination={false} size="small" />
                    </FormCard>

                    <FormCard id="section-sensitive" title="6. 敏感字段">
                        <Table dataSource={sensitiveFields} columns={sensitiveFieldColumns} pagination={false} size="small" />
                    </FormCard>

                    <FormCard id="section-sql" title="7. SQL 逻辑">
                        <SqlBlockEditor blocks={sqlBlocks} onChange={setSqlBlocks} />
                    </FormCard>

                    <FormCard id="section-caveats" title="8. 注意事项">
                        {caveats.map((c, i) => (
                            <div key={i} className="mb-2 border p-2 rounded">
                                <Input value={c.title} onChange={e => {
                                    const nc = [...caveats]; nc[i].title = e.target.value; setCaveats(nc);
                                }} className="mb-1 font-bold" placeholder="标题" />
                                <TextArea value={c.text} onChange={e => {
                                    const nc = [...caveats]; nc[i].text = e.target.value; setCaveats(nc);
                                }} placeholder="内容" />
                                <Button danger size="small" type="link" onClick={() => setCaveats(caveats.filter((_, idx) => idx !== i))}>删除</Button>
                            </div>
                        ))}
                        <Button type="dashed" block onClick={() => setCaveats([...caveats, { id: `c-${Date.now()}`, title: '新增事项', text: '', risk: 'LOW' }])}>
                            添加事项
                        </Button>
                    </FormCard>

                </Form>
            </EditorLayout>

            {/* Import Drawer */}
            <ImportDrawer
                open={importDrawerVisible}
                onOpenChange={setImportDrawerVisible}
                onApplyDraft={applyImportedContent}
            />

            {/* Metadata Drawer */}
            <Drawer
                title="搜索表"
                open={metadataDrawerVisible}
                onClose={() => setMetadataDrawerVisible(false)}
                width={600}
            >
                {/* Simplified Metadata Search UI reuse */}
                <div className="flex gap-2 mb-4">
                    <Input value={tableSearchKeyword} onChange={e => setTableSearchKeyword(e.target.value)} onPressEnter={searchMetadataTables} placeholder="输入表名" />
                    <Button type="primary" onClick={searchMetadataTables} loading={searchingTables}>搜索</Button>
                </div>
                <Table
                    dataSource={tableSearchResults}
                    columns={[
                        { title: '表名', dataIndex: 'tableFullname' },
                        { title: '操作', render: (_, r) => <Button size="small" onClick={() => addTable(r)}>添加</Button> }
                    ]}
                    size="small"
                />
            </Drawer>

            {/* Publish Modal reuse */}
            <Modal
                title="发布确认"
                open={publishModalVisible}
                onOk={handlePublishConfirm}
                onCancel={() => setPublishModalVisible(false)}
                confirmLoading={publishing}
                okText="确认发布"
            >
                <Form form={publishForm} layout="vertical">
                    <Form.Item label="验证日期" name="lastVerifiedAt" rules={[{ required: true }]}><DatePicker className="w-full" /></Form.Item>
                    <Form.Item label="验证人" name="verifiedBy" rules={[{ required: true }]} initialValue="user"><Input /></Form.Item>
                    <Form.Item label="验证凭证" name="verifyEvidence"><Input placeholder="工单号/测试报告等" /></Form.Item>
                    <Form.Item label="变更摘要" name="changeSummary" rules={[{ required: true }]}><TextArea /></Form.Item>
                </Form>
            </Modal>
        </>
    )
}

function EmptyState({ text }: { text: string }) {
    return <div className="text-muted-foreground text-center py-4 text-xs">{text}</div>
}

export default SceneEditorPage
