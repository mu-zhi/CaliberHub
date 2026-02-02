import React from 'react';
import { cn } from '@/lib/utils';
import {
    CheckCircleOutlined,
    ClockCircleOutlined,
    InboxOutlined,
    LockOutlined,
    WarningOutlined,
    CloseCircleOutlined,
} from '@ant-design/icons';

/**
 * 统一的状态徽标组件
 * 强制约束颜色使用，避免彩虹效果
 */

export type SceneStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type BadgeKind = SceneStatus | 'SENSITIVE' | 'EXPIRING' | 'EXPIRED' | 'BLOCKED';

interface StatusBadgeProps {
    kind: BadgeKind;
    label?: string;
    className?: string;
    showIcon?: boolean;
}

const CONFIG: Record<BadgeKind, {
    style: string;
    icon: React.ReactNode;
    defaultLabel: string;
    ariaLabel: string;
}> = {
    PUBLISHED: {
        style: 'bg-success/15 text-success border border-success/20',
        icon: <CheckCircleOutlined />,
        defaultLabel: '已发布',
        ariaLabel: '状态：已发布',
    },
    DRAFT: {
        style: 'bg-muted text-muted-foreground border border-border',
        icon: <ClockCircleOutlined />,
        defaultLabel: '草稿',
        ariaLabel: '状态：草稿',
    },
    ARCHIVED: {
        style: 'bg-secondary text-secondary-foreground border border-border',
        icon: <InboxOutlined />,
        defaultLabel: '已归档',
        ariaLabel: '状态：已归档',
    },
    SENSITIVE: {
        style: 'bg-sensitive/15 text-sensitive border border-sensitive/20',
        icon: <LockOutlined />,
        defaultLabel: '敏感',
        ariaLabel: '包含敏感信息',
    },
    EXPIRING: {
        style: 'bg-warning/15 text-warning border border-warning/20',
        icon: <WarningOutlined />,
        defaultLabel: '即将过期',
        ariaLabel: '验证即将过期',
    },
    EXPIRED: {
        style: 'bg-warning/15 text-warning border border-warning/20',
        icon: <WarningOutlined />,
        defaultLabel: '已过期',
        ariaLabel: '验证已过期',
    },
    BLOCKED: {
        style: 'bg-danger/15 text-danger border border-danger/20',
        icon: <CloseCircleOutlined />,
        defaultLabel: '已阻止',
        ariaLabel: '状态：已阻止',
    },
};

export const StatusBadge: React.FC<StatusBadgeProps> = ({
    kind,
    label,
    className,
    showIcon = true,
}) => {
    const config = CONFIG[kind];
    const displayLabel = label || config.defaultLabel;

    return (
        <span
            role="status"
            aria-label={config.ariaLabel}
            className={cn(
                'inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium',
                config.style,
                className
            )}
        >
            {showIcon && <span className="text-[10px]">{config.icon}</span>}
            {displayLabel}
        </span>
    );
};

export default StatusBadge;
