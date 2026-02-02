import { useState, useEffect } from 'react'
import {
    Card,
    Table,
    Button,
    Space,
    Typography,
    Tag,
    Drawer,
    Form,
    Input,
    message,
    Modal
} from 'antd'
import {
    PlusOutlined,
    EditOutlined,
    DeleteOutlined,
    ReloadOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { domainApi } from '@/api'
import type { Domain } from '@/types'
import dayjs from 'dayjs'

const { Title } = Typography

function DomainsPage() {
    const [loading, setLoading] = useState(false)
    const [domains, setDomains] = useState<Domain[]>([])
    const [drawerVisible, setDrawerVisible] = useState(false)
    const [editingDomain, setEditingDomain] = useState<Domain | null>(null)
    const [submitting, setSubmitting] = useState(false)
    const [form] = Form.useForm()

    useEffect(() => {
        loadData()
    }, [])

    const loadData = async () => {
        setLoading(true)
        try {
            const res = await domainApi.list()
            if (res.data.data) {
                setDomains(res.data.data)
            }
        } catch (error) {
            message.error('领域列表加载失败')
        } finally {
            setLoading(false)
        }
    }

    const handleAdd = () => {
        setEditingDomain(null)
        form.resetFields()
        setDrawerVisible(true)
    }

    const handleEdit = (record: Domain) => {
        setEditingDomain(record)
        form.setFieldsValue(record)
        setDrawerVisible(true)
    }

    const handleDelete = (record: Domain) => {
        Modal.confirm({
            title: '确认删除',
            content: `确定要删除 "${record.name}" 吗？该操作不可恢复。`,
            okText: '删除',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
                try {
                    await domainApi.delete(record.id)
                    message.success('删除成功')
                    loadData()
                } catch {
                    message.error('删除失败')
                }
            },
        })
    }

    const handleSubmit = async () => {
        try {
            const values = await form.validateFields()
            setSubmitting(true)
            if (editingDomain) {
                await domainApi.update(editingDomain.id, values)
                message.success('更新成功')
            } else {
                await domainApi.create(values)
                message.success('创建成功')
            }
            setDrawerVisible(false)
            loadData()
        } catch (error) {
            // Form validation error
        } finally {
            setSubmitting(false)
        }
    }

    const columns: ColumnsType<Domain> = [
        {
            title: '领域标识',
            dataIndex: 'domainKey',
            key: 'domainKey',
            render: (text: string) => <Tag color="blue">{text}</Tag>,
        },
        {
            title: '领域名称',
            dataIndex: 'name',
            key: 'name',
            render: (text: string) => <b>{text}</b>,
        },
        {
            title: '说明',
            dataIndex: 'description',
            key: 'description',
            ellipsis: true,
        },
        {
            title: '场景数',
            dataIndex: 'sceneCount',
            key: 'sceneCount',
            width: 100,
            align: 'center',
            render: (cnt: number) => <Tag>{cnt || 0}</Tag>
        },
        {
            title: '更新时间',
            dataIndex: 'updatedAt',
            key: 'updatedAt',
            width: 150,
            render: (date: string) => date ? dayjs(date).format('YYYY-MM-DD') : '-'
        },
        {
            title: '操作',
            key: 'action',
            width: 150,
            render: (_, record) => (
                <Space>
                    <Button
                        type="text"
                        size="small"
                        icon={<EditOutlined />}
                        onClick={() => handleEdit(record)}
                    >
                        编辑
                    </Button>
                    <Button
                        type="text"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(record)}
                    >
                        删除
                    </Button>
                </Space>
            ),
        },
    ]

    return (
        <div style={{ padding: 24 }}>
            <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Title level={4} style={{ margin: 0 }}>领域治理</Title>
                <Space>
                    <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
                        刷新
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
                        新建领域
                    </Button>
                </Space>
            </div>
            <Card>
                <Table
                    columns={columns}
                    dataSource={domains}
                    rowKey="id"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                />
            </Card>

            <Drawer
                title={editingDomain ? '编辑领域' : '新建领域'}
                open={drawerVisible}
                onClose={() => setDrawerVisible(false)}
                width={480}
                extra={
                    <Space>
                        <Button onClick={() => setDrawerVisible(false)}>取消</Button>
                        <Button type="primary" onClick={handleSubmit} loading={submitting}>
                            保存
                        </Button>
                    </Space>
                }
            >
                <Form form={form} layout="vertical">
                    <Form.Item
                        name="domainKey"
                        label="领域标识"
                        rules={[
                            { required: true, message: '请输入领域标识' },
                            { pattern: /^[a-z_]+$/, message: '只能使用小写字母和下划线' },
                        ]}
                    >
                        <Input placeholder="例如 retail_banking" disabled={!!editingDomain} />
                    </Form.Item>
                    <Form.Item
                        name="name"
                        label="领域名称"
                        rules={[{ required: true, message: '请输入领域名称' }]}
                    >
                        <Input placeholder="例如 零售银行" />
                    </Form.Item>
                    <Form.Item
                        name="description"
                        label="说明"
                    >
                        <Input.TextArea rows={4} placeholder="领域范围说明..." />
                    </Form.Item>
                </Form>
            </Drawer>
        </div>
    )
}

export default DomainsPage
