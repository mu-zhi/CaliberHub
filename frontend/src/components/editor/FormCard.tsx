import React from 'react';
import { Card } from 'antd';

interface FormCardProps {
    id?: string;
    title: React.ReactNode;
    extra?: React.ReactNode;
    children: React.ReactNode;
    className?: string;
}

const FormCard: React.FC<FormCardProps> = ({ id, title, extra, children, className }) => {
    return (
        <div id={id} className={`scroll-mt-20 ${className}`}>
            <Card
                title={title}
                extra={extra}
                className="shadow-sm border-gray-200 hover:shadow-md transition-shadow"
                headStyle={{ borderBottom: '1px solid #f0f0f0', minHeight: '48px' }}
                bodyStyle={{ padding: '24px' }}
            >
                {children}
            </Card>
        </div>
    );
};

export default FormCard;
