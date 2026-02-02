import React, { useState } from 'react'
import { Button, ButtonProps, message } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'

interface ActionButtonProps extends Omit<ButtonProps, 'onClick'> {
    onClickAsync: (e: React.MouseEvent<HTMLElement>) => Promise<any>
    successMessage?: string
    showSuccessIcon?: boolean
    restoreDelay?: number // Time in ms to restore from Success/Error state to Idle
}

const ActionButton: React.FC<ActionButtonProps> = ({
    onClickAsync,
    children,
    successMessage,
    showSuccessIcon = false,
    restoreDelay = 2000,
    ...props
}) => {
    const [loading, setLoading] = useState(false)
    const [status, setStatus] = useState<'idle' | 'success' | 'error'>('idle')

    const handleClick = async (e: React.MouseEvent<HTMLElement>) => {
        if (loading) return

        setLoading(true)
        setStatus('idle')

        try {
            await onClickAsync(e)

            if (successMessage) {
                message.success(successMessage)
            }

            setStatus('success')

            if (showSuccessIcon || restoreDelay > 0) {
                setTimeout(() => {
                    setStatus('idle')
                }, restoreDelay)
            }
        } catch (error: any) {
            console.error('Action failed:', error)
            setStatus('error')

            const errorMsg = error.message || '操作失败'
            message.error(errorMsg)

            setTimeout(() => {
                setStatus('idle')
            }, restoreDelay)
        } finally {
            setLoading(false)
        }
    }

    const getIcon = () => {
        if (status === 'success' && showSuccessIcon) return <CheckCircleOutlined />
        if (status === 'error') return <CloseCircleOutlined />
        return props.icon
    }

    return (
        <Button
            {...props}
            loading={loading}
            icon={getIcon()}
            onClick={handleClick}
            disabled={props.disabled || loading}
        >
            {children}
        </Button>
    )
}

export default ActionButton
