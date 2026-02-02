import {
    Card,
    Typography,
    Form,
    Input,
    Button,
    Space,
    Tabs,
    List,
    Tag,
    Alert,
    Switch,
    message,
} from 'antd'
import {
    SaveOutlined,

    ApiOutlined,
    SafetyCertificateOutlined,
    ExportOutlined,
    UserOutlined,
    AuditOutlined,
} from '@ant-design/icons'
import ActionButton from '@/components/ActionButton'
import { settingsApi } from '@/api'
import { useState } from 'react'

const { Title, Text } = Typography

function SettingsPage() {

    // Test Connection Action
    const handleTestConnection = async () => {
        await settingsApi.testMetadata()
    }

    const lintRules = [
        { id: 'E001', name: '缺必填项', description: '口径定义为空', type: 'error' },
        { id: 'E002', name: '表名未匹配', description: 'SQL 抽取的表在元数据平台 NOT_FOUND', type: 'error' },
        { id: 'E003', name: '黑名单表', description: 'SQL 中使用了黑名单表', type: 'error' },
        { id: 'E004', name: '敏感字段未声明', description: 'has_sensitive=false 但用到敏感表', type: 'error' },
        { id: 'E005', name: '验证过期', description: '发布时 last_verified_at 超过 180 天', type: 'error' },
        { id: 'W001', name: 'SELECT *', description: 'SQL 使用了 SELECT *', type: 'warning' },
        { id: 'W002', name: '无 LIMIT', description: 'SQL 无 LIMIT 子句', type: 'warning' },
    ]
    const [rulesState, setRulesState] = useState(lintRules.map(r => ({ ...r, enabled: true })))

    const handleRuleToggle = (id: string, checked: boolean) => {
        setRulesState(prev => prev.map(r => r.id === id ? { ...r, enabled: checked } : r))
    }

    const handleRuleReset = () => {
        setRulesState(lintRules.map(r => ({ ...r, enabled: true })))
        message.success('已恢复默认规则开关')
    }

    const handleRuleSave = () => {
        // TODO: POST to backend when available
        message.success('规则已保存（当前为前端模拟）')
    }

    const tabItems = [
        {
            key: 'metadata',
            label: (
                <span>
                    <ApiOutlined />
                    元数据连接
                </span>
            ),
            children: (
                <Form layout="vertical" initialValues={{ baseUrl: 'https://metadata.example.com', timeout: 3000 }}>
                    <Alert message="仅管理员可修改元数据连接配置" type="warning" showIcon style={{ marginBottom: 24 }} />
                    <Form.Item label="元数据服务地址 (Base URL)" name="baseUrl" rules={[{ required: true }]}>
                        <Input placeholder="https://..." />
                    </Form.Item>
                    <Form.Item label="连接超时 (ms)" name="timeout">
                        <Input type="number" />
                    </Form.Item>
                    <Form.Item label="认证凭证 (Token Ref)" name="credentialRef">
                        <Input.Password placeholder="ENV_VAR_REF_KEY" />
                    </Form.Item>

                    <Space style={{ marginTop: 16 }}>
                        <ActionButton
                            onClickAsync={handleTestConnection}
                            successMessage="连接测试成功"
                        >
                            测试连接
                        </ActionButton>
                        <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>
                            保存配置
                        </Button>
                    </Space>
                </Form>
            ),
        },
        {
            key: 'lint',
            label: (
                <span>
                    <SafetyCertificateOutlined />
                    校验门禁
                </span>
            ),
            children: (
                <div>
                    <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
                        <Text type="secondary">配置发布前的强制校验规则 (Blocking Errors) 和 警告建议 (Warnings)</Text>
                        <Space>
                            <Button onClick={handleRuleReset}>恢复默认</Button>
                            <Button type="primary" onClick={handleRuleSave}>保存规则</Button>
                        </Space>
                    </div>
                    <List
                        header={<div style={{ fontWeight: 'bold' }}>启用规则</div>}
                        bordered
                        dataSource={rulesState}
                        renderItem={item => (
                            <List.Item actions={[<Switch checked={item.enabled} onChange={(checked) => handleRuleToggle(item.id, checked)} />]}>
                                <List.Item.Meta
                                    title={
                                        <Space>
                                            <Tag color={item.type === 'error' ? 'red' : 'orange'}>
                                                {item.type.toUpperCase()}
                                            </Tag>
                                            <Text strong>{item.id}</Text>
                                            <Text>{item.name}</Text>
                                        </Space>
                                    }
                                    description={item.description}
                                />
                            </List.Item>
                        )}
                    />
                </div>
            ),
        },
        {
            key: 'export',
            label: (
                <span>
                    <ExportOutlined />
                    导出与RAG
                </span>
            ),
            children: (
                <Form layout="vertical">
                    <Title level={5}>导出 Schema 设置</Title>
                    <Form.Item label="文档结构版本 (Doc Schema)">
                        <Input value="v1.0" disabled />
                    </Form.Item>
                    <Form.Item label="RAG 切片版本 (Chunk Schema)">
                        <Input value="v1.0" disabled />
                    </Form.Item>
                    <Button type="primary" icon={<SaveOutlined />}>保存导出配置</Button>
                </Form>
            ),
        },
        {
            key: 'iam',
            label: (
                <span>
                    <UserOutlined />
                    权限与角色
                </span>
            ),
            children: (
                <div style={{ textAlign: 'center', padding: 50 }}>
                    <Text type="secondary">Coming Soon: RBAC Role Management</Text>
                </div>
            ),
        },
        {
            key: 'audit',
            label: (
                <span>
                    <AuditOutlined />
                    审计与留痕
                </span>
            ),
            children: (
                <div style={{ textAlign: 'center', padding: 50 }}>
                    <Text type="secondary">Coming Soon: Global Audit Log Retention Policies</Text>
                </div>
            ),
        },
    ]

    return (
        <div>
            <Title level={2} style={{ marginBottom: 24 }}>系统设置</Title>
            <Card>
                <Tabs items={tabItems} tabPosition="left" />
            </Card>
        </div>
    )
}

export default SettingsPage
