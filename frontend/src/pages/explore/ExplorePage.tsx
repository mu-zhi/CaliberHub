import React, { useMemo, useState } from 'react';
import { Button, Empty, Select, Typography, Space, Pagination } from 'antd';
import { PlusOutlined, CloudUploadOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import FacetPanel, { FacetPanelValue } from '@/components/common/FacetPanel';
import SceneResultCard from '@/components/common/SceneResultCard';
import GlobalSearchBar from '@/components/common/GlobalSearchBar';
import ImportDrawer from '@/components/business/ImportDrawer';
import dayjs from 'dayjs';
import type { ImportCommitResponse } from '@/types/import';

const { Text } = Typography;
// Mock data
const MOCK_SCENES = [
    {
        id: '1',
        title: '用户活跃度分析',
        description: '统计日活与留存，并按地区/设备拆解。',
        domain: '增长',
        status: 'PUBLISHED' as const,
        owner: 'Alice',
        updatedAt: '2025-01-28',
        lastVerifiedAt: '2025-01-15',
        verifiedBy: 'Alice',
        tables: ['dwd_user_activity', 'dws_user_retention'],
        hasSensitive: false,
    },
    {
        id: '2',
        title: 'Q1 收入预测',
        description: '基于历史趋势的季度收入预测草案。',
        domain: '金融',
        status: 'DRAFT' as const,
        owner: 'Bob',
        updatedAt: '2025-01-29',
        lastVerifiedAt: '2024-10-01',
        verifiedBy: 'Bob',
        tables: ['ads_revenue_daily'],
        hasSensitive: true,
    },
    {
        id: '3',
        title: '物流延迟报告',
        description: '识别超过3天的订单延迟并输出清单。',
        domain: '物流',
        status: 'PUBLISHED' as const,
        owner: 'Charlie',
        updatedAt: '2025-01-20',
        lastVerifiedAt: '2023-12-01',
        verifiedBy: 'QA',
        tables: ['ods_order', 'dwd_delivery_delay'],
        hasSensitive: false,
    }
];

const ExplorePage: React.FC = () => {
    const navigate = useNavigate();
    const [importOpen, setImportOpen] = useState(false);
    const [searchText, setSearchText] = useState('');
    const [facets, setFacets] = useState<FacetPanelValue>({
        domains: [],
        statuses: [],
        hasSensitiveOnly: false,
        verification: [],
    });
    const [sortBy, setSortBy] = useState<'verified_desc' | 'updated_desc'>('verified_desc');
    const [scenes, setScenes] = useState(MOCK_SCENES);

    const handleCreate = () => {
        // In real app, create draft via API then navigate
        // For now, just navigate to a new ID
        const newId = 'new-' + Date.now();
        navigate(`/scenes/${newId}/edit`);
    };

    const filteredScenes = useMemo(() => {
        const list = scenes.filter((s) => {
            const keywordMatch =
                s.title.toLowerCase().includes(searchText.toLowerCase()) ||
                s.description.toLowerCase().includes(searchText.toLowerCase());
            const domainMatch = facets.domains.length === 0 || facets.domains.includes(s.domain);
            const statusMatch = facets.statuses.length === 0 || facets.statuses.includes(s.status);
            const sensitiveMatch = !facets.hasSensitiveOnly || s.hasSensitive;
            const daysSinceVerify = s.lastVerifiedAt ? dayjs().diff(dayjs(s.lastVerifiedAt), 'day') : undefined;
            const verificationMatch =
                facets.verification.length === 0 ||
                facets.verification.some(v => {
                    if (v === 'unverified') return !s.lastVerifiedAt;
                    if (!daysSinceVerify && v !== 'unverified') return false;
                    if (v === 'valid') return (daysSinceVerify ?? Infinity) <= 90;
                    if (v === 'warning') return (daysSinceVerify ?? Infinity) > 90 && (daysSinceVerify ?? Infinity) <= 365;
                    if (v === 'expired') return (daysSinceVerify ?? Infinity) > 365;
                    return false;
                });

            return keywordMatch && domainMatch && statusMatch && sensitiveMatch && verificationMatch;
        });

        return list.sort((a, b) => {
            if (sortBy === 'verified_desc') {
                const av = a.lastVerifiedAt ? dayjs(a.lastVerifiedAt).valueOf() : 0;
                const bv = b.lastVerifiedAt ? dayjs(b.lastVerifiedAt).valueOf() : 0;
                return bv - av;
            }
            // updated_desc
            return dayjs(b.updatedAt).valueOf() - dayjs(a.updatedAt).valueOf();
        });
    }, [searchText, facets, sortBy, scenes]);

    const pageSize = 9;
    const [page, setPage] = useState(1);

    const pagedScenes = useMemo(() => {
        const start = (page - 1) * pageSize;
        return filteredScenes.slice(start, start + pageSize);
    }, [filteredScenes, page]);

    return (
        <div className="h-full flex flex-col">
            {/* Top Bar */}
            <div className="flex justify-between items-center mb-6">
                <GlobalSearchBar onSearch={setSearchText} onChange={setSearchText} />
                <div className="flex items-center gap-3">
                            <Space size="small" align="center">
                                <Select
                                    size="middle"
                                    value={sortBy}
                                    style={{ width: 200 }}
                            options={[
                                { label: '按最近验证', value: 'verified_desc' },
                                { label: '按最近更新', value: 'updated_desc' },
                            ]}
                            onChange={setSortBy}
                        />
                        <Text type="secondary">共 {filteredScenes.length} 条</Text>
                    </Space>
                    <Button icon={<CloudUploadOutlined />} onClick={() => setImportOpen(true)}>
                        导入文档
                    </Button>
                    <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
                        新建场景
                    </Button>
                </div>
            </div>

            {/* Main Content */}
            <div className="flex flex-1 overflow-hidden">
                {/* Left Facets */}
                <FacetPanel value={facets} onChange={setFacets} />

                {/* Results Grid */}
                <div className="flex-1 overflow-y-auto pr-2 flex flex-col">
                    {filteredScenes.length > 0 ? (
                        <>
                            <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-4">
                                {pagedScenes.map(scene => (
                                    <SceneResultCard
                                        key={scene.id}
                                        {...scene}
                                        onView={() => navigate(`/scenes/${scene.id}`)}
                                        onEdit={() => navigate(`/scenes/${scene.id}/edit`)}
                                    />
                                ))}
                            </div>
                            <div className="mt-4 self-end">
                                <Pagination
                                    current={page}
                                    pageSize={pageSize}
                                    total={filteredScenes.length}
                                    onChange={setPage}
                                    size="small"
                                    showSizeChanger={false}
                                />
                            </div>
                        </>
                    ) : (
                        <div className="h-full flex items-center justify-center">
                            <Empty
                                description="未找到匹配的场景"
                                imageStyle={{ height: 80 }}
                            >
                                <Space>
                                    <Button type="primary" onClick={() => setImportOpen(true)}>导入文档</Button>
                                    <Button onClick={handleCreate}>新建场景</Button>
                                    <Button type="link" onClick={() => {
                                        setFacets({ domains: [], statuses: [], hasSensitiveOnly: false, verification: [] });
                                        setSearchText('');
                                        setPage(1);
                                    }}>清除筛选</Button>
                                </Space>
                            </Empty>
                        </div>
                    )}
                </div>
            </div>

            {/* Drawers/Dialogs */}
            <ImportDrawer
                open={importOpen}
                onOpenChange={setImportOpen}
                onImported={(res: ImportCommitResponse) => {
                    const newScenes = (res.createdScenes || []).map((item) => ({
                        id: item.sceneCode,
                        title: item.sceneCode,
                        description: '导入的场景草稿',
                        domain: '未分配',
                        status: 'DRAFT' as const,
                        owner: '导入用户',
                        updatedAt: dayjs().format('YYYY-MM-DD'),
                        hasSensitive: true,
                    }));
                    if (newScenes.length > 0) {
                        setScenes((prev) => [...newScenes, ...prev]);
                        setPage(1);
                    }
                    setImportOpen(false);
                }}
            />
        </div>
    );
};

export default ExplorePage;
