import { useState, useEffect } from 'react'
import {
    Card,
    Typography,
    Switch,
    Select,
    InputNumber,
    Button,
    List,
    Tag,
    message,
    Space,
    Spin
} from 'antd'
import { SaveOutlined, ReloadOutlined } from '@ant-design/icons'
import { settingsApi } from '@/api'

const { Title, Text } = Typography

interface LintRule {
    id: string
    name: string
    enabled: boolean
    severity: 'error' | 'warning'
    value?: number
}

function LintSettingsPage() {
    const [loading, setLoading] = useState(false)
    const [saving, setSaving] = useState(false)
    const [rules, setRules] = useState<LintRule[]>([])

    useEffect(() => {
        loadSettings()
    }, [])

    const loadSettings = async () => {
        setLoading(true)
        try {
            const res = await settingsApi.getLintSettings()
            if (res.data.data && res.data.data.rules) {
                setRules(res.data.data.rules)
            }
        } catch (error) {
            message.error('校验规则加载失败')
        } finally {
            setLoading(false)
        }
    }

    const handleSave = async () => {
        setSaving(true)
        try {
            await settingsApi.saveLintSettings({ rules })
            message.success('规则已保存')
        } catch (error) {
            message.error('保存失败')
        } finally {
            setSaving(false)
        }
    }

    const updateRule = (id: string, updates: Partial<LintRule>) => {
        setRules(rules.map(r => r.id === id ? { ...r, ...updates } : r))
    }

    return (
        <div style={{ padding: 24 }}>
            <Card
                title={<Title level={4} style={{ margin: 0 }}>校验门禁</Title>}
                extra={
                    <Space>
                        <Button icon={<ReloadOutlined />} onClick={loadSettings} loading={loading}>重置</Button>
                        <Button type="primary" icon={<SaveOutlined />} onClick={handleSave} loading={saving}>
                            保存
                        </Button>
                    </Space>
                }
            >
                {loading ? <div className="text-center p-8"><Spin /></div> : (
                    <List
                        dataSource={rules}
                        renderItem={item => (
                            <List.Item
                                actions={[
                                    <Space key="severity">
                                        <Text type="secondary">级别:</Text>
                                        <Select
                                            value={item.severity}
                                            style={{ width: 100 }}
                                            onChange={(val) => updateRule(item.id, { severity: val })}
                                            disabled={!item.enabled}
                                            options={[
                                                { label: '错误', value: 'error' },
                                                { label: '警告', value: 'warning' },
                                            ]}
                                        />
                                    </Space>,
                                    item.value !== undefined && (
                                        <Space key="value">
                                            <Text type="secondary">阈值:</Text>
                                            <InputNumber
                                                value={item.value}
                                                onChange={(val) => updateRule(item.id, { value: val || 0 })}
                                                disabled={!item.enabled}
                                                style={{ width: 80 }}
                                            />
                                        </Space>
                                    ),
                                    <Switch
                                        key="toggle"
                                        checked={item.enabled}
                                        onChange={(checked) => updateRule(item.id, { enabled: checked })}
                                    />
                                ]}
                            >
                                <List.Item.Meta
                                    title={
                                        <Space>
                                            <Text strong={item.enabled} type={item.enabled ? undefined : 'secondary'}>
                                                {item.name}
                                            </Text>
                                            {item.enabled && <Tag color={item.severity === 'error' ? 'red' : 'orange'}>{item.severity === 'error' ? '错误' : '警告'}</Tag>}
                                        </Space>
                                    }
                                    description={`规则ID: ${item.id}`}
                                />
                            </List.Item>
                        )}
                    />
                )}
            </Card>
        </div>
    )
}

export default LintSettingsPage
