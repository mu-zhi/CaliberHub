import { useState, useEffect } from 'react'
import { Modal, Tabs, Typography, Button, Space, Spin, message, Table, Tag } from 'antd'
import { CopyOutlined, ReloadOutlined } from '@ant-design/icons'
import { importFormatApi, ImportFormatSchema, ImportFormatTemplate } from '../api'

const { Text, Paragraph } = Typography

interface ImportFormatModalProps {
    open: boolean
    onClose: () => void
}

type TemplateMode = 'empty' | 'example_sql' | 'example_rule'

/**
 * 导入格式说明弹窗
 * 
 * 展示当前系统支持的导入格式 Schema 和 Template。
 */
export function ImportFormatModal({ open, onClose }: ImportFormatModalProps) {
    const [loading, setLoading] = useState(false)
    const [schema, setSchema] = useState<ImportFormatSchema | null>(null)
    const [template, setTemplate] = useState<ImportFormatTemplate | null>(null)
    const [templateMode, setTemplateMode] = useState<TemplateMode>('empty')
    const [activeTab, setActiveTab] = useState('template')

    // 加载数据
    useEffect(() => {
        if (open) {
            loadData()
        }
    }, [open, templateMode])

    const loadData = async () => {
        setLoading(true)
        try {
            const [schemaRes, templateRes] = await Promise.all([
                importFormatApi.getSchema(),
                importFormatApi.getTemplate('v1', templateMode)
            ])
            setSchema(schemaRes.data)
            setTemplate(templateRes.data)
        } catch (error) {
            console.error('加载导入格式失败:', error)
            message.error('加载导入格式失败')
        } finally {
            setLoading(false)
        }
    }

    // 复制到剪贴板
    const handleCopy = (content: unknown, label: string) => {
        const text = JSON.stringify(content, null, 2)
        navigator.clipboard.writeText(text).then(() => {
            message.success(`${label} 已复制到剪贴板`)
        }).catch(() => {
            message.error('复制失败')
        })
    }

    // 从 Schema 生成字段说明表格数据
    const generateFieldDocs = () => {
        if (!schema?.$defs) return []

        const docs: Array<{
            key: string
            section: string
            field: string
            type: string
            required: boolean
            description: string
        }> = []

        // 解析 Scene 定义
        const sceneDef = schema.$defs.Scene as { properties?: Record<string, { type?: string | string[], description?: string }>, required?: string[] }
        if (sceneDef?.properties) {
            Object.entries(sceneDef.properties).forEach(([field, def]) => {
                docs.push({
                    key: `scene-${field}`,
                    section: 'Scene',
                    field,
                    type: Array.isArray(def.type) ? def.type.join(' | ') : (def.type || 'object'),
                    required: sceneDef.required?.includes(field) || false,
                    description: def.description || ''
                })
            })
        }

        // 解析 SqlBlock 定义
        const sqlBlockDef = schema.$defs.SqlBlock as { properties?: Record<string, { type?: string | string[], description?: string }>, required?: string[] }
        if (sqlBlockDef?.properties) {
            Object.entries(sqlBlockDef.properties).forEach(([field, def]) => {
                docs.push({
                    key: `sql-${field}`,
                    section: 'SqlBlock',
                    field,
                    type: Array.isArray(def.type) ? def.type.join(' | ') : (def.type || 'object'),
                    required: sqlBlockDef.required?.includes(field) || false,
                    description: def.description || ''
                })
            })
        }

        return docs
    }

    const fieldColumns = [
        {
            title: '模块',
            dataIndex: 'section',
            key: 'section',
            width: 100,
            render: (text: string) => <Tag>{text}</Tag>
        },
        {
            title: '字段',
            dataIndex: 'field',
            key: 'field',
            width: 150,
            render: (text: string) => <Text code>{text}</Text>
        },
        {
            title: '类型',
            dataIndex: 'type',
            key: 'type',
            width: 100
        },
        {
            title: '必填',
            dataIndex: 'required',
            key: 'required',
            width: 60,
            render: (required: boolean) => required ? <Tag color="red">是</Tag> : <Tag>否</Tag>
        },
        {
            title: '说明',
            dataIndex: 'description',
            key: 'description'
        }
    ]

    const tabItems = [
        {
            key: 'template',
            label: '模板',
            children: (
                <div>
                    <Space style={{ marginBottom: 12 }}>
                        <Text>模板类型：</Text>
                        <Button
                            size="small"
                            type={templateMode === 'empty' ? 'primary' : 'default'}
                            onClick={() => setTemplateMode('empty')}
                        >
                            空模板
                        </Button>
                        <Button
                            size="small"
                            type={templateMode === 'example_sql' ? 'primary' : 'default'}
                            onClick={() => setTemplateMode('example_sql')}
                        >
                            SQL 示例
                        </Button>
                        <Button
                            size="small"
                            type={templateMode === 'example_rule' ? 'primary' : 'default'}
                            onClick={() => setTemplateMode('example_rule')}
                        >
                            规则示例
                        </Button>
                        <Button
                            icon={<CopyOutlined />}
                            onClick={() => handleCopy(template, 'Template')}
                            disabled={!template}
                        >
                            复制
                        </Button>
                    </Space>
                    <Paragraph>
                        <pre style={{
                            background: '#f5f5f5',
                            padding: 16,
                            borderRadius: 8,
                            maxHeight: 400,
                            overflow: 'auto',
                            fontSize: 12
                        }}>
                            {template ? JSON.stringify(template, null, 2) : '加载中...'}
                        </pre>
                    </Paragraph>
                </div>
            )
        },
        {
            key: 'schema',
            label: 'Schema',
            children: (
                <div>
                    <Space style={{ marginBottom: 12 }}>
                        <Text type="secondary">JSON Schema Draft 2020-12</Text>
                        <Button
                            icon={<CopyOutlined />}
                            onClick={() => handleCopy(schema, 'Schema')}
                            disabled={!schema}
                        >
                            复制
                        </Button>
                    </Space>
                    <Paragraph>
                        <pre style={{
                            background: '#f5f5f5',
                            padding: 16,
                            borderRadius: 8,
                            maxHeight: 400,
                            overflow: 'auto',
                            fontSize: 12
                        }}>
                            {schema ? JSON.stringify(schema, null, 2) : '加载中...'}
                        </pre>
                    </Paragraph>
                </div>
            )
        },
        {
            key: 'fields',
            label: '字段说明',
            children: (
                <div>
                    <Paragraph type="secondary" style={{ marginBottom: 12 }}>
                        以下是导入格式中各字段的说明，从 Schema 自动生成。
                    </Paragraph>
                    <Table
                        dataSource={generateFieldDocs()}
                        columns={fieldColumns}
                        size="small"
                        pagination={false}
                        scroll={{ y: 350 }}
                    />
                </div>
            )
        }
    ]

    return (
        <Modal
            title="导入格式说明"
            open={open}
            onCancel={onClose}
            width={800}
            footer={[
                <Button key="refresh" icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
                    刷新
                </Button>,
                <Button key="close" type="primary" onClick={onClose}>
                    关闭
                </Button>
            ]}
        >
            <Spin spinning={loading}>
                <div style={{ marginBottom: 12 }}>
                    <Text type="secondary">
                        当前版本：{schema?.schema_version || '-'} |
                        文档类型：<Text code>{schema?.title || '-'}</Text>
                    </Text>
                </div>
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    items={tabItems}
                />
            </Spin>
        </Modal>
    )
}

export default ImportFormatModal
