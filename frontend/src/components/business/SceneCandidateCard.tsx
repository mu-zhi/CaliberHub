import React, { useState } from 'react';
import { Card, Checkbox, Tag, Button, Collapse, Descriptions, Table, Alert, Typography, Space, Divider } from 'antd';
import { EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import type { SceneCandidate } from '../../types/import';

const { Text, Paragraph } = Typography;
const { Panel } = Collapse;

interface SceneCandidateCardProps {
  candidate: SceneCandidate;
  selected: boolean;
  onToggle: () => void;
}

export const SceneCandidateCard: React.FC<SceneCandidateCardProps> = ({
  candidate,
  selected,
  onToggle,
}) => {
  const [showDetails, setShowDetails] = useState(false);

  const { draftContent } = candidate;

  const handleCardClick = (e: React.MouseEvent) => {
    // Don't toggle selection when clicking on detail buttons or collapse panels
    if ((e.target as HTMLElement).closest('.ant-btn, .ant-collapse, .ant-table')) {
      return;
    }
    onToggle();
  };

  const renderBasicInfo = () => (
    <Descriptions size="small" column={2}>
      <Descriptions.Item label="场景标题">
        {draftContent.title || draftContent.sceneTitle || '（无标题）'}
      </Descriptions.Item>
      <Descriptions.Item label="贡献者">
        {draftContent.contributors?.join(', ') || '-'}
      </Descriptions.Item>
      <Descriptions.Item label="场景描述" span={2}>
        <Paragraph ellipsis={{ rows: 2, expandable: true }}>
          {draftContent.sceneDescription || '-'}
        </Paragraph>
      </Descriptions.Item>
    </Descriptions>
  );

  const renderInputParams = () => {
    if (!draftContent.inputs?.params?.length) {
      return <Text type="secondary">无输入参数</Text>;
    }

    return (
      <Table
        dataSource={draftContent.inputs.params}
        columns={[
          { title: '参数名', dataIndex: 'name', width: 120 },
          { title: '示例', dataIndex: 'example', width: 150 },
          { title: '描述', dataIndex: 'description', ellipsis: true },
        ]}
        pagination={false}
        size="small"
        rowKey="name"
      />
    );
  };

  const renderSqlBlocks = () => {
    if (!draftContent.sqlBlocks?.length) {
      return <Text type="secondary">无SQL块</Text>;
    }

    return (
      <div className="space-y-3">
        {draftContent.sqlBlocks.map((block, index) => (
          <div key={index}>
            <Text strong>{block.name || `SQL块 ${index + 1}`}</Text>
            <pre className="bg-gray-50 p-3 rounded text-xs mt-1 overflow-x-auto border border-gray-200 max-h-32">
              {block.sql}
            </pre>
          </div>
        ))}
      </div>
    );
  };

  const renderSourceTables = () => {
    if (!draftContent.sourceTablesHint?.length) {
      return <Text type="secondary">无来源表信息</Text>;
    }

    return (
      <Table
        dataSource={draftContent.sourceTablesHint}
        columns={[
          { title: '表名', dataIndex: 'table', width: 200 },
          { title: '置信度', dataIndex: 'confidence', width: 80, render: (val) => `${Math.round(val * 100)}%` },
          { title: '描述', dataIndex: 'description', ellipsis: true },
        ]}
        pagination={false}
        size="small"
        rowKey="table"
      />
    );
  };

  const renderCaveats = () => {
    if (!draftContent.caveats?.length) {
      return <Text type="secondary">无注意事项</Text>;
    }

    return (
      <div className="space-y-2">
        {draftContent.caveats.map((caveat, index) => (
          <Alert
            key={index}
            message={caveat.text}
            type={caveat.level === 'HIGH' ? 'error' : caveat.level === 'MEDIUM' ? 'warning' : 'info'}
            showIcon
            size="small"
          />
        ))}
      </div>
    );
  };

  return (
    <Card
      size="small"
      className={`transition-all border ${selected ? 'border-blue-500 bg-blue-50' : 'border-gray-200 hover:border-blue-300'}`}
    >
      <div className="flex items-start gap-3" onClick={handleCardClick}>
        <Checkbox checked={selected} />
        <div className="flex-1">
          {/* Header */}
          <div className="flex items-center justify-between mb-2">
            <div className="font-bold text-base">
              {candidate.titleGuess || <Text type="secondary">（无标题）</Text>}
            </div>
            <Button
              type="text"
              size="small"
              icon={showDetails ? <EyeInvisibleOutlined /> : <EyeOutlined />}
              onClick={(e) => {
                e.stopPropagation();
                setShowDetails(!showDetails);
              }}
            >
              {showDetails ? '收起详情' : '查看详情'}
            </Button>
          </div>

          {/* Stats */}
          <Space size="large" className="text-gray-500 text-xs mb-2">
            <span>SQL块: {candidate.parseStats.sqlBlocks}</span>
            <span>抽取表: {candidate.parseStats.tablesExtracted}</span>
            <span>输入参数: {draftContent.inputs?.params?.length || 0}</span>
            <span>注意事项: {draftContent.caveats?.length || 0}</span>
          </Space>

          {/* Warnings */}
          <div className="mb-2">
            {candidate.warnings.map((w, i) => (
              <Tag key={i} color="warning" className="mr-1 mb-1">{w}</Tag>
            ))}
          </div>

          {/* Quick Preview */}
          {!showDetails && draftContent.sceneDescription && (
            <Paragraph ellipsis={{ rows: 2 }} className="text-sm text-gray-600 mb-0">
              {draftContent.sceneDescription}
            </Paragraph>
          )}

          {/* Detailed Content */}
          {showDetails && (
            <div className="mt-4 border-t pt-4">
              <Collapse ghost>
                <Panel header="基本信息" key="basic">
                  {renderBasicInfo()}
                </Panel>
                
                <Panel header="口径定义" key="definition">
                  <div>
                    <Text strong>适用范围：</Text>
                    <Paragraph>{draftContent.applicability || '-'}</Paragraph>
                    <Text strong>边界条件：</Text>
                    <Paragraph>{draftContent.boundaries || '-'}</Paragraph>
                    <Text strong>口径定义：</Text>
                    <Paragraph>{draftContent.caliberDefinition || '-'}</Paragraph>
                  </div>
                </Panel>

                <Panel header={`输入参数 (${draftContent.inputs?.params?.length || 0})`} key="inputs">
                  {renderInputParams()}
                </Panel>

                <Panel header="输出定义" key="outputs">
                  <Paragraph>{draftContent.outputs?.summary || '-'}</Paragraph>
                  {draftContent.outputs?.fields?.length > 0 && (
                    <div>
                      <Text strong>输出字段：</Text>
                      <div className="mt-2">
                        {draftContent.outputs.fields.map((field, index) => (
                          <Tag key={index} className="mb-1">{field}</Tag>
                        ))}
                      </div>
                    </div>
                  )}
                </Panel>

                <Panel header={`SQL逻辑 (${draftContent.sqlBlocks?.length || 0}块)`} key="sql">
                  {renderSqlBlocks()}
                </Panel>

                <Panel header={`来源表 (${draftContent.sourceTablesHint?.length || 0})`} key="tables">
                  {renderSourceTables()}
                </Panel>

                <Panel header={`注意事项 (${draftContent.caveats?.length || 0})`} key="caveats">
                  {renderCaveats()}
                </Panel>
              </Collapse>
            </div>
          )}
        </div>
      </div>
    </Card>
  );
};

export default SceneCandidateCard;
