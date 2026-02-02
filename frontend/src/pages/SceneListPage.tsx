import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
    Table,
    Card,
    Button,
    Input,
    Select,
    Space,
    Tag,
    Tooltip,
    Typography,
    message,
    Modal,
    Spin,
} from 'antd'
import {
    PlusOutlined,
    SearchOutlined,
    EditOutlined,
    EyeOutlined,
    DeleteOutlined,
    LockOutlined,
    CheckCircleOutlined,
    ExclamationCircleOutlined,
    CloseCircleOutlined,
    ImportOutlined,
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import { sceneApi, domainApi } from '../api'
import type { Scene, Domain } from '../types'
import ImportDrawer from '../components/business/ImportDrawer'
import { ImportFormatModal } from './ImportFormatModal'

const { Text } = Typography

function SceneListPage() {
    const navigate = useNavigate()
    const [loading, setLoading] = useState(true)
    const [scenes, setScenes] = useState<Scene[]>([])
    const [domains, setDomains] = useState<Domain[]>([])
    const [searchKeyword, setSearchKeyword] = useState('')
    const [selectedDomain, setSelectedDomain] = useState<string | undefined>()
    const [deprecateModalVisible, setDeprecateModalVisible] = useState(false)
    const [deprecateReason, setDeprecateReason] = useState('')
    const [selectedScene, setSelectedScene] = useState<Scene | null>(null)
    const [importDrawerOpen, setImportDrawerOpen] = useState(false)
    const [formatModalOpen, setFormatModalOpen] = useState(false)

    // 加载数据
    useEffect(() => {
        loadData()
    }, [])

    const loadData = async () => {
        setLoading(true)
        try {
            const [sceneRes, domainRes] = await Promise.all([
                sceneApi.list(),
                domainApi.list(),
            ])
            if (sceneRes.data.data) {
                setScenes(sceneRes.data.data)
            }
            if (domainRes.data.data) {
                setDomains(domainRes.data.data)
            }
        } catch (error) {
            console.error('加载数据失败:', error)
            message.error('加载数据失败')
        } finally {
            setLoading(false)
        }
    }

    // 搜索
    const handleSearch = async () => {
        setLoading(true)
        try {
            const res = await sceneApi.list({
                domainId: selectedDomain,
                keyword: searchKeyword,
            })
            if (res.data.data) {
                setScenes(res.data.data)
            }
        } catch (error) {
            message.error('搜索失败')
        } finally {
            setLoading(false)
        }
    }

    // 计算验证状态
    const getVerifiedStatus = (lastVerifiedAt?: string) => {
        if (!lastVerifiedAt) return null
        const days = dayjs().diff(dayjs(lastVerifiedAt), 'day')
        if (days <= 90) return { color: 'success', icon: <CheckCircleOutlined />, text: '有效' }
        if (days <= 365) return { color: 'warning', icon: <ExclamationCircleOutlined />, text: '即将过期' }
        return { color: 'error', icon: <CloseCircleOutlined />, text: '已过期' }
    }

    // 新建
    const handleCreate = async () => {
        setLoading(true)
        try {
            const res = await sceneApi.create({ title: '新建场景' })
            if (res.data.data) {
                message.success('创建成功')
                navigate(`/scenes/${res.data.data.sceneCode}/edit`)
            }
        } catch (error) {
            message.error('创建失败')
        } finally {
            setLoading(false)
        }
    }

    // 编辑
    const handleEdit = async (record: Scene) => {
        if (record.versionStatus === 'PUBLISHED') {
            // 如果只有已发布版本，自动创建草稿
            setLoading(true)
            try {
                await sceneApi.createVersion(record.sceneCode)
                navigate(`/scenes/${record.sceneCode}/edit`)
            } catch (error) {
                message.error('无法创建新版本草稿')
            } finally {
                setLoading(false)
            }
        } else {
            navigate(`/scenes/${record.sceneCode}/edit`)
        }
    }

    // 废弃
    const handleDeprecate = async () => {
        if (!selectedScene) return
        try {
            await sceneApi.deprecate(selectedScene.sceneCode, deprecateReason)
            message.success('已废弃')
            setDeprecateModalVisible(false)
            setSelectedScene(null)
            setDeprecateReason('')
            loadData()
        } catch (error) {
            message.error('废弃失败')
        }
    }

    const columns: ColumnsType<Scene> = [
        {
            title: '场景码',
            dataIndex: 'sceneCode',
            key: 'sceneCode',
            width: 150,
            render: (text) => <Text code copyable={{ text }}>{text}</Text>,
        },
        {
            title: '标题',
            dataIndex: 'title',
            key: 'title',
            render: (text, record) => (
                <Space>
                    <a onClick={() => navigate(`/scenes/${record.sceneCode}`)}>{text}</a>
                    {record.hasSensitive && (
                        <Tooltip title="包含敏感字段">
                            <Tag color="orange" icon={<LockOutlined />}>敏感</Tag>
                        </Tooltip>
                    )}
                </Space>
            ),
        },
        {
            title: '领域',
            dataIndex: 'domainName',
            key: 'domainName',
            width: 100,
            render: (text) => <Tag>{text}</Tag>,
        },
        {
            title: '验证状态',
            dataIndex: 'lastVerifiedAt',
            key: 'lastVerifiedAt',
            width: 130,
            render: (_, record) => {
                const status = getVerifiedStatus(record.lastVerifiedAt)
                if (!status) return <Text type="secondary">未验证</Text>
                return (
                    <Tag color={status.color} icon={status.icon}>
                        {dayjs(record.lastVerifiedAt).format('YYYY-MM-DD')}
                    </Tag>
                )
            },
        },
        {
            title: '负责人',
            dataIndex: 'ownerUser',
            key: 'ownerUser',
            width: 100,
        },
        {
            title: '标签',
            dataIndex: 'tags',
            key: 'tags',
            width: 160,
            render: (tags: string[]) => (
                <>
                    {tags?.slice(0, 2).map((tag) => (
                        <Tag key={tag}>{tag}</Tag>
                    ))}
                    {tags?.length > 2 && <Tag>+{tags.length - 2}</Tag>}
                </>
            ),
        },
        {
            title: '更新时间',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            width: 120,
            render: (text) => dayjs(text).format('YYYY-MM-DD'),
        },
        {
            title: '操作',
            key: 'action',
            width: 150,
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="查看">
                        <Button
                            type="text"
                            size="small"
                            icon={<EyeOutlined />}
                            onClick={() => navigate(`/scenes/${record.sceneCode}`)}
                        />
                    </Tooltip>
                    <Tooltip title="编辑">
                        <Button
                            type="text"
                            size="small"
                            icon={<EditOutlined />}
                            onClick={() => handleEdit(record)}
                        />
                    </Tooltip>
                    <Tooltip title="废弃">
                        <Button
                            type="text"
                            size="small"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => {
                                setSelectedScene(record)
                                setDeprecateModalVisible(true)
                            }}
                        />
                    </Tooltip>
                </Space>
            ),
        },
    ]

    return (
        <div>
            <Card
                title="场景列表"
                extra={
                    <Space>
                        <Button onClick={() => setFormatModalOpen(true)}>
                            导入格式
                        </Button>
                        <Button icon={<ImportOutlined />} onClick={() => setImportDrawerOpen(true)}>
                            导入旧文档
                        </Button>
                        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                            新建场景
                        </Button>
                    </Space>
                }
            >
                {/* 搜索栏 */}
                <Space style={{ marginBottom: 16 }} wrap>
                    <Select
                        placeholder="选择领域"
                        style={{ width: 160 }}
                        allowClear
                        value={selectedDomain}
                        onChange={setSelectedDomain}
                        options={domains.map((d) => ({ value: d.id, label: d.name }))}
                    />
                    <Input
                        placeholder="搜索场景码/标题"
                        style={{ width: 240 }}
                        value={searchKeyword}
                        onChange={(e) => setSearchKeyword(e.target.value)}
                        onPressEnter={handleSearch}
                    />
                    <Button icon={<SearchOutlined />} onClick={handleSearch}>
                        搜索
                    </Button>
                </Space>

                {/* 表格 */}
                <Spin spinning={loading}>
                    <Table
                        dataSource={scenes}
                        columns={columns}
                        rowKey="id"
                        pagination={{
                            showSizeChanger: true,
                            showQuickJumper: true,
                            showTotal: (total) => `共 ${total} 条`,
                        }}
                    />
                </Spin>
            </Card>

            {/* 废弃确认弹窗 */}
            <Modal
                title="确认废弃"
                open={deprecateModalVisible}
                onOk={handleDeprecate}
                onCancel={() => {
                    setDeprecateModalVisible(false)
                    setSelectedScene(null)
                    setDeprecateReason('')
                }}
                okText="确认废弃"
                okButtonProps={{ danger: true }}
            >
                <p>确定要废弃场景 <Text strong>{selectedScene?.title}</Text> 吗？</p>
                <p style={{ color: '#666' }}>废弃后该场景将不再显示在默认列表中，但可以通过过滤器查看。</p>
                <Input.TextArea
                    placeholder="请输入废弃原因（可选）"
                    value={deprecateReason}
                    onChange={(e) => setDeprecateReason(e.target.value)}
                    rows={3}
                    style={{ marginTop: 16 }}
                />
            </Modal>

            {/* 导入抽屉 */}
            <ImportDrawer
                open={importDrawerOpen}
                onOpenChange={setImportDrawerOpen}
                onImported={(res: any) => {
                    message.success(`成功导入 ${res.createdScenes.length} 个场景`)
                    loadData()
                    if (res.createdScenes[0]) {
                        navigate(`/scenes/${res.createdScenes[0].sceneCode}/edit`)
                    }
                }}
            />

            {/* 导入格式说明弹窗 */}
            <ImportFormatModal
                open={formatModalOpen}
                onClose={() => setFormatModalOpen(false)}
            />
        </div>
    )
}

export default SceneListPage
