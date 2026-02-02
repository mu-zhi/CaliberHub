import React, { useMemo, useState } from 'react'
import { Card, Table, Typography, Tag, Space, Button, message } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import ImportDrawer from '@/components/business/ImportDrawer'
import type { ImportCommitResponse } from '@/types/import'
import { useNavigate } from 'react-router-dom'
import type { SceneDraftContent } from '@/types/import'

const { Title } = Typography

type ImportTask = {
    key: string
    taskId: string
    createdAt: string
    sourceType: string
    status: string
    sceneTitle: string
    sceneCode?: string
    draftVersionId?: string
}

const ImportsPage: React.FC = () => {
    const [importDrawerVisible, setImportDrawerVisible] = useState(false)
    const navigate = useNavigate()
    const [draftMap, setDraftMap] = useState<Record<string, SceneDraftContent>>({})
    const [pendingDrafts, setPendingDrafts] = useState<SceneDraftContent[] | null>(null)

    const [tasks, setTasks] = useState(() => [
        {
            key: 'imp-001',
            taskId: 'imp-001',
            createdAt: '2025-01-29 10:00:00',
            sourceType: 'PASTE',
            status: 'COMPLETED',
            sceneTitle: '示例场景 A',
            sceneCode: 'SC-DEMO-001',
        },
    ])

    const columns = [
        {
            title: '任务ID',
            dataIndex: 'taskId',
            key: 'taskId',
        },
        {
            title: '创建时间',
            dataIndex: 'createdAt',
            key: 'createdAt',
        },
        {
            title: '来源类型',
            dataIndex: 'sourceType',
            key: 'sourceType',
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            render: (status: string) => {
                let color = 'default'
                if (status === 'COMPLETED') color = 'success'
                if (status === 'FAILED') color = 'error'
                if (status === 'PROCESSING') color = 'processing'
                return <Tag color={color}>{status}</Tag>
            }
        },
        {
            title: '目标场景',
            dataIndex: 'sceneTitle',
            key: 'sceneTitle',
        },
        {
            title: '操作',
            key: 'action',
            render: (_: any, record: ImportTask) => (
                <Space size="middle">
                    <Button
                        type="link"
                        size="small"
                        onClick={() => {
                            const code = (record as any).sceneCode || (record as any).sceneTitle
                            if (code) {
                                navigate(`/scenes/${code}/edit`, {
                                    state: { importedDraft: draftMap[code] },
                                })
                            } else {
                                message.info('当前任务没有可跳转的场景')
                            }
                        }}
                    >
                        查看详情
                    </Button>
                </Space>
            ),
        },
    ]

    const dataSource = useMemo(() => tasks, [tasks])

    return (
        <div style={{ padding: 24 }}>
            <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={2}>导入中心</Title>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    onClick={() => setImportDrawerVisible(true)}
                >
                    新建导入
                </Button>
            </div>

            <Card>
                <Table dataSource={dataSource} columns={columns} />
            </Card>

            <ImportDrawer
                open={importDrawerVisible}
                onOpenChange={setImportDrawerVisible}
                onCommittedDrafts={(drafts) => setPendingDrafts(drafts)}
                onImported={(res: ImportCommitResponse) => {
                    const now = new Date()
                    const newTaskId = `imp-${now.getTime()}`
                    const createdScenes = res.createdScenes ?? []
                    const firstScene = createdScenes[0]

                    if (pendingDrafts && createdScenes.length) {
                        const mapping: Record<string, SceneDraftContent> = {}
                        createdScenes.forEach((scene, idx) => {
                            mapping[scene.sceneCode] = pendingDrafts[idx] || pendingDrafts[0]
                        })
                        setDraftMap(prev => ({ ...mapping, ...prev }))
                        setPendingDrafts(null)
                    }

                    setTasks(prev => [
                        {
                            key: newTaskId,
                            taskId: newTaskId,
                            createdAt: now.toISOString().replace('T', ' ').split('.')[0],
                            sourceType: 'PASTE',
                            status: 'COMPLETED',
                            sceneTitle: firstScene?.sceneCode || '-',
                            sceneCode: firstScene?.sceneCode,
                            draftVersionId: firstScene?.draftVersionId,
                        },
                        ...prev,
                    ])
                    message.success(`已导入 ${createdScenes.length || 0} 个场景`)
                    setImportDrawerVisible(false)
                }}
            />
        </div >
    )
}

export default ImportsPage
