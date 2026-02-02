import React from 'react';
import { Tabs, Badge } from 'antd';


interface RightPanelTabsProps {
    lintContent: React.ReactNode;
    tablesContent: React.ReactNode;
    completionContent: React.ReactNode;
    lintErrorCount?: number;
    tableCount?: number;
}

const RightPanelTabs: React.FC<RightPanelTabsProps> = ({
    lintContent,
    tablesContent,
    completionContent,
    lintErrorCount = 0,
    tableCount = 0
}) => {
    const items = [
        {
            key: 'lint',
            label: (
                <span className="flex items-center gap-1">
                    门禁
                    {lintErrorCount > 0 && (
                        <Badge count={lintErrorCount} size="small" className="ml-1" />
                    )}
                </span>
            ),
            children: <div className="p-4">{lintContent}</div>,
        },
        {
            key: 'tables',
            label: (
                <span className="flex items-center gap-1">
                    来源表
                    {tableCount > 0 && <span className="text-xs text-gray-400">({tableCount})</span>}
                </span>
            ),
            children: <div className="p-4">{tablesContent}</div>,
        },
        {
            key: 'completion',
            label: '完成度',
            children: <div className="p-4">{completionContent}</div>,
        },
    ];

    return (
        <Tabs
            defaultActiveKey="lint"
            items={items}
            className="h-full bg-card [&>.ant-tabs-nav]:px-4 [&>.ant-tabs-nav]:mb-0 border-b border-border"
        />
    );
};

export default RightPanelTabs;
