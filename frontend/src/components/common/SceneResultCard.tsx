import React from 'react';
import { Card, Typography, Button, Dropdown, Space } from 'antd';
import { MoreOutlined, EditOutlined, EyeOutlined, ExportOutlined, CopyOutlined, DeleteOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { TrustBadge } from './TrustBadge';
import { StatusBadge } from './StatusBadge';
import { cn } from '@/lib/utils';

const { Text, Title, Paragraph } = Typography;

export interface SceneResultCardProps {
    id: string;
    title: string;
    description: string;
    domain: string;
    status: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
    owner: string;
    updatedAt: string;
    lastVerifiedAt?: string;
    verifiedBy?: string;
    hasSensitive?: boolean;
    tables?: string[];
    onEdit?: () => void;
    onView?: () => void;
}

const SceneResultCard: React.FC<SceneResultCardProps> = ({
    title,
    description,
    domain,
    status,
    owner,
    updatedAt,
    lastVerifiedAt,
    verifiedBy,
    hasSensitive,
    tables = [],
    onEdit,
    onView
}) => {
    const menuItems: MenuProps['items'] = [
        { key: 'export_doc', label: '导出 doc.json', icon: <ExportOutlined /> },
        { key: 'export_chunks', label: '导出 chunks.json', icon: <ExportOutlined /> },
        { key: 'copy_link', label: '复制链接', icon: <CopyOutlined /> },
        { key: 'deprecate', label: '废弃', icon: <DeleteOutlined />, danger: true, disabled: status !== 'PUBLISHED' },
    ];

    return (
        <Card
            className={cn(
                "w-full border-border bg-card transition-all duration-150",
                "hover:shadow-md hover:-translate-y-0.5"
            )}
            bodyStyle={{ padding: '16px 20px' }}
        >
            <div className="flex justify-between items-start mb-2">
                <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                        {/* Domain uses muted gray - no rainbow colors */}
                        <span className="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium bg-muted text-muted-foreground border border-border">
                            {domain}
                        </span>
                        {hasSensitive && <StatusBadge kind="SENSITIVE" />}
                        <StatusBadge kind={status} />
                    </div>
                    <Title level={4} className="mb-1 !text-lg hover:text-primary cursor-pointer transition-colors" onClick={onView}>
                        {title}
                    </Title>
                    <Paragraph className="text-muted-foreground mb-3 line-clamp-2 h-10" ellipsis={{ rows: 2 }}>
                        {description}
                    </Paragraph>
                    <div className="flex flex-wrap gap-2 text-xs text-muted-foreground mb-2">
                        <TrustBadge lastVerifiedAt={lastVerifiedAt} verifiedBy={verifiedBy} />
                        <span className="text-muted-foreground">更新: {updatedAt}</span>
                        {tables.length > 0 && (
                            <Space size={4} wrap>
                                {tables.slice(0, 3).map(t => (
                                    <span key={t} className="inline-flex items-center rounded px-1.5 py-0.5 text-xs bg-muted text-muted-foreground">{t}</span>
                                ))}
                                {tables.length > 3 && <span className="text-muted-foreground">+{tables.length - 3}</span>}
                            </Space>
                        )}
                    </div>
                </div>
                <Dropdown menu={{ items: menuItems }} placement="bottomRight">
                    <Button type="text" icon={<MoreOutlined />} className="text-muted-foreground hover:text-foreground" />
                </Dropdown>
            </div>

            <div className="flex justify-between items-center text-xs text-muted-foreground border-t border-border pt-3">
                <div className="flex gap-4">
                    <span>负责人: <Text type="secondary">{owner}</Text></span>
                </div>
                <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <Button type="text" size="small" icon={<EyeOutlined />} onClick={onView}>查看</Button>
                    <Button type="text" size="small" icon={<EditOutlined />} onClick={onEdit}>编辑</Button>
                </div>
            </div>
        </Card>
    );
};

export default SceneResultCard;
