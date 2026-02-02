import React, { useState } from 'react';
import { Tabs, Empty } from 'antd';
import { useNavigate } from 'react-router-dom';
import SceneResultCard, { SceneResultCardProps } from '@/components/common/SceneResultCard';
import GlobalSearchBar from '@/components/common/GlobalSearchBar';

const MOCK_MY_DRAFTS: SceneResultCardProps[] = [
    {
        id: '2',
        title: 'Q1 收入预测',
        description: '基于历史趋势的季度收入预测草案。',
        domain: '金融',
        status: 'DRAFT',
        owner: 'Me',
        updatedAt: '2025-01-29',
        hasSensitive: true,
    }
];

const MOCK_MY_OWNED: SceneResultCardProps[] = [
    {
        id: '1',
        title: '用户活跃度分析',
        description: '统计日活与留存，并按地区/设备拆解。',
        domain: '增长',
        status: 'PUBLISHED',
        owner: 'Me',
        updatedAt: '2025-01-28',
        hasSensitive: false,
    }
];

const WorkbenchPage: React.FC = () => {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('drafts');

    const renderList = (data: SceneResultCardProps[]) => {
        if (data.length === 0) {
            return <Empty description="暂无场景" />;
        }
        return (
            <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
                {data.map(scene => (
                    <SceneResultCard
                        key={scene.id}
                        {...scene}
                        onView={() => navigate(`/scenes/${scene.id}`)}
                        onEdit={() => navigate(`/scenes/${scene.id}/edit`)}
                    />
                ))}
            </div>
        );
    };

    const items = [
        {
            key: 'drafts',
            label: '我的草稿',
            children: renderList(MOCK_MY_DRAFTS),
        },
        {
            key: 'owned',
            label: '我负责的',
            children: renderList(MOCK_MY_OWNED),
        },
        {
            key: 'expiring',
            label: '即将过期',
            children: <Empty description="暂无即将过期场景" />,
        },
        {
            key: 'blocked',
            label: '被门禁拦截',
            children: <Empty description="暂无被拦截场景" />,
        },
    ];

    return (
        <div className="p-6 h-full flex flex-col">
            <div className="mb-6 flex justify-between items-center">
                <h1 className="text-2xl font-bold m-0">编辑台</h1>
                <div className="w-[300px]">
                    <GlobalSearchBar placeholder="搜索我的场景..." />
                </div>
            </div>

            <div className="flex-1 overflow-hidden flex flex-col">
                <Tabs
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    items={items}
                    className="h-full flex flex-col [&>.ant-tabs-content]:flex-1 [&>.ant-tabs-content]:overflow-y-auto [&>.ant-tabs-content-holder]:flex [&>.ant-tabs-content-holder]:flex-col [&>.ant-tabs-content-holder]:overflow-hidden"
                />
            </div>
        </div>
    );
};

export default WorkbenchPage;
