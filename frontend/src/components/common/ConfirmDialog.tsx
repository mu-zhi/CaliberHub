import React from 'react';
import { Modal } from 'antd';
import { ExclamationCircleFilled } from '@ant-design/icons';

const { confirm } = Modal;

interface ConfirmDialogProps {
    title: string;
    content: string;
    onOk: () => Promise<void> | void;
    okText?: string;
    cancelText?: string;
    danger?: boolean;
}

export const showConfirm = ({ title, content, onOk, okText = '确定', cancelText = '取消', danger = false }: ConfirmDialogProps) => {
    confirm({
        title,
        icon: <ExclamationCircleFilled />,
        content,
        okText,
        okType: danger ? 'danger' : 'primary',
        cancelText,
        onOk,
    });
};

/**
 * Component version if needed, but the function is specialized for imperative use.
 * For now we just export the utility function.
 */
const ConfirmDialog: React.FC = () => {
    return null;
};

export default ConfirmDialog;
