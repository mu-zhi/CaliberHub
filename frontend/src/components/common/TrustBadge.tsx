import { CheckCircleOutlined, ExclamationCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'

export type TrustStatus = 'valid' | 'warning' | 'expired' | 'unverified'

export interface TrustBadgeProps {
    lastVerifiedAt?: string
    verifiedBy?: string
}

export function computeTrustStatus(lastVerifiedAt?: string): TrustStatus {
    if (!lastVerifiedAt) return 'unverified'
    const days = dayjs().diff(dayjs(lastVerifiedAt), 'day')
    if (days <= 90) return 'valid'
    if (days <= 365) return 'warning'
    return 'expired'
}

export const TrustBadge = ({ lastVerifiedAt, verifiedBy }: TrustBadgeProps) => {
    const status = computeTrustStatus(lastVerifiedAt)
    const map: Record<TrustStatus, {
        style: string;
        icon: React.ReactNode;
        text: string;
        ariaLabel: string;
    }> = {
        valid: {
            style: 'bg-success/15 text-success border border-success/20',
            icon: <CheckCircleOutlined />,
            text: '已验证',
            ariaLabel: '验证状态：有效'
        },
        warning: {
            style: 'bg-warning/15 text-warning border border-warning/20',
            icon: <ExclamationCircleOutlined />,
            text: '即将过期',
            ariaLabel: '验证状态：即将过期'
        },
        expired: {
            style: 'bg-danger/15 text-danger border border-danger/20',
            icon: <CloseCircleOutlined />,
            text: '已过期',
            ariaLabel: '验证状态：已过期'
        },
        unverified: {
            style: 'bg-warning/15 text-warning border border-warning/20',
            icon: <ExclamationCircleOutlined />,
            text: '未验证',
            ariaLabel: '验证状态：未验证'
        },
    }
    const { style, icon, text, ariaLabel } = map[status]
    const label = lastVerifiedAt ? `${text} · ${lastVerifiedAt}${verifiedBy ? ` · ${verifiedBy}` : ''}` : text
    return (
        <span
            role="status"
            aria-label={ariaLabel}
            className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${style}`}
        >
            <span className="text-[10px]">{icon}</span>
            {label}
        </span>
    )
}

export default TrustBadge
