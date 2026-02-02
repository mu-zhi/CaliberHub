import React from 'react';
import { Checkbox, Collapse, Typography } from 'antd';
import { CaretRightOutlined } from '@ant-design/icons';

const { Panel } = Collapse;
const { Title } = Typography;

export interface FacetPanelValue {
    domains: string[];
    statuses: string[];
    hasSensitiveOnly: boolean;
    verification: string[]; // valid | warning | expired | unverified
}

interface FacetPanelProps {
    value: FacetPanelValue;
    onChange: (value: FacetPanelValue) => void;
}

const FacetPanel: React.FC<FacetPanelProps> = ({ value, onChange }) => {
    // Mock data for facets
    const domains = [
        { label: '零售', value: '零售' },
        { label: '金融', value: '金融' },
        { label: '物流', value: '物流' },
    ];

    const statusOptions = [
        { label: '草稿', value: 'DRAFT' },
        { label: '已发布', value: 'PUBLISHED' },
        { label: '已归档', value: 'ARCHIVED' },
    ];

    const handleDomainsChange = (list: any[]) => {
        onChange({ ...value, domains: list as string[] });
    };
    const handleStatusChange = (list: any[]) => {
        onChange({ ...value, statuses: list as string[] });
    };
    const handleSensitiveChange = (checked: boolean) => {
        onChange({ ...value, hasSensitiveOnly: checked });
    };
    const handleVerificationChange = (list: any[]) => {
        onChange({ ...value, verification: list as string[] });
    };

    return (
        <div className="w-64 flex-shrink-0 mr-6">
            <div className="mb-4">
                <Title level={5}>筛选</Title>
            </div>
            <Collapse
                bordered={false}
                defaultActiveKey={['1', '2']}
                expandIcon={({ isActive }) => <CaretRightOutlined rotate={isActive ? 90 : 0} />}
                className="bg-transparent"
                ghost
            >
                <Panel header="领域" key="1" className="mb-2">
                    <Checkbox.Group
                        className="flex flex-col gap-2"
                        options={domains}
                        value={value.domains}
                        onChange={handleDomainsChange}
                    />
                </Panel>
                <Panel header="状态" key="2" className="mb-2">
                    <Checkbox.Group
                        className="flex flex-col gap-2"
                        options={statusOptions}
                        value={value.statuses}
                        onChange={handleStatusChange}
                    />
                </Panel>
                <Panel header="合规" key="3">
                    <div className="flex flex-col gap-2">
                        <Checkbox
                            checked={value.hasSensitiveOnly}
                            onChange={(e) => handleSensitiveChange(e.target.checked)}
                        >
                            含敏感字段
                        </Checkbox>
                    </div>
                </Panel>
                <Panel header="验证状态" key="4">
                    <Checkbox.Group
                        className="flex flex-col gap-2"
                        options={[
                            { label: '有效(≤90天)', value: 'valid' },
                            { label: '即将过期(90-365天)', value: 'warning' },
                            { label: '已过期(>365天)', value: 'expired' },
                            { label: '未验证', value: 'unverified' },
                        ]}
                        value={value.verification}
                        onChange={handleVerificationChange}
                    />
                </Panel>
            </Collapse>
        </div>
    );
};

export default FacetPanel;
