import React, { useState, useCallback } from 'react';
import { Drawer, Steps, Radio, Input, Button, Card, Tag, Alert, Checkbox, Typography, Space, Upload } from 'antd';
import { UploadOutlined, FileTextOutlined, AppstoreOutlined, CheckCircleOutlined } from '@ant-design/icons';
import type {
  ImportParseRequest,
  ImportParseResponse,
  ImportCommitRequest,
  ImportCommitResponse,
  ImportMode,
  ImportSourceType,
  SceneCandidate, // Keep existing imports
  SceneDraftContent
} from '../../types/import';
import { SceneCandidateCard } from './SceneCandidateCard';

export interface ImportDrawerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defaultMode?: ImportMode;
  defaultSourceType?: ImportSourceType;
  maxBytes?: number;
  onImported?: (response: ImportCommitResponse) => void;
  onApplyDraft?: (draft: SceneDraftContent) => void;
  defaultDomainId?: string;
  onCommittedDrafts?: (drafts: SceneDraftContent[]) => void;
}

const { TextArea } = Input;
const { Title, Text } = Typography;

const API_BASE = '/api';

export const ImportDrawer: React.FC<ImportDrawerProps> = ({
  open,
  onOpenChange,
  defaultMode = 'single_scene',
  defaultSourceType = 'PASTE_MD',
  maxBytes = 1_000_000,
  onImported,
  onApplyDraft, // New prop
  defaultDomainId,
  onCommittedDrafts,
}) => {
  // State
  const [currentStep, setCurrentStep] = useState<0 | 1>(0); // 0: Input, 1: Preview
  const [status, setStatus] = useState<'idle' | 'parsing' | 'preview' | 'committing' | 'done' | 'error'>('idle');
  const [sourceType, setSourceType] = useState<ImportSourceType>(defaultSourceType);
  const [mode, setMode] = useState<ImportMode>(defaultMode);
  const [rawText, setRawText] = useState('');
  const [parseResponse, setParseResponse] = useState<ImportParseResponse | null>(null);
  const [selectedTempIds, setSelectedTempIds] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [commitResponse, setCommitResponse] = useState<ImportCommitResponse | null>(null);
  const [uploadedFileName, setUploadedFileName] = useState<string | null>(null);

  const reset = useCallback(() => {
    setCurrentStep(0);
    setStatus('idle');
    setRawText('');
    setUploadedFileName(null);
    setParseResponse(null);
    setSelectedTempIds([]);
    setError(null);
    setCommitResponse(null);
  }, []);

  const handleClose = useCallback(() => {
    reset();
    onOpenChange(false);
  }, [reset, onOpenChange]);

  const handleParse = useCallback(async () => {
    if (!rawText.trim()) {
      setError('请输入文档内容');
      return;
    }

    if (rawText.length > maxBytes) {
      setError(`文档过大，最大支持 ${Math.floor(maxBytes / 1024)} KB`);
      return;
    }

    setStatus('parsing');
    setError(null);

    try {
      const req: ImportParseRequest = {
        sourceType,
        rawText,
        mode
      };

      const res = await fetch(`${API_BASE}/import/parse`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req)
      });

      if (!res.ok) {
        const text = await res.text(); // Try to get error message
        throw new Error(`解析失败: ${res.status} ${text}`);
      }

      const data: ImportParseResponse = await res.json();
      setParseResponse(data);
      setSelectedTempIds(data.sceneCandidates.map(c => c.tempId));
      setCurrentStep(1);
      setStatus('preview');
    } catch (err) {
      console.error(err);
      setError(err instanceof Error ? err.message : '解析失败');
      setStatus('error');
    }
  }, [rawText, maxBytes, sourceType, mode]);

  const handleFile = async (file: File) => {
    if (file.size > maxBytes) {
      setError(`文件过大，最大支持 ${Math.floor(maxBytes / 1024)} KB`);
      return Upload.LIST_IGNORE;
    }
    try {
      const text = await file.text();
      setRawText(text);
      setUploadedFileName(file.name);
      setError(null);
    } catch {
      setError('读取文件失败');
    }
    // Prevent auto upload; we only parse locally then call backend parse.
    return false;
  };

  const handleCommit = useCallback(async () => {
    if (selectedTempIds.length === 0) {
      setError('请选择至少一个场景');
      return;
    }

    // If in "Apply" mode
    if (onApplyDraft && parseResponse) {
      const candidate = parseResponse.sceneCandidates.find(c => c.tempId === selectedTempIds[0]);
      if (candidate) {
        onApplyDraft(candidate.draftContent);
        handleClose();
        return;
      }
    }

    setStatus('committing');
    setError(null);

    try {
      const req: ImportCommitRequest = {
        sourceType,
        rawText,
        mode,
        selectedTempIds,
        defaultDomainId
      };

      const res = await fetch(`${API_BASE}/import/commit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(req)
      });

      if (!res.ok) {
        throw new Error(`导入失败: ${res.status}`);
      }

      const data: ImportCommitResponse = await res.json();
      setCommitResponse(data);
      setStatus('done');

      if (onCommittedDrafts && parseResponse) {
        const drafts = parseResponse.sceneCandidates
          .filter(c => selectedTempIds.includes(c.tempId))
          .map(c => c.draftContent);
        onCommittedDrafts(drafts);
      }

      if (onImported) {
        onImported(data);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : '导入失败');
      setStatus('error');
    }
  }, [selectedTempIds, sourceType, rawText, mode, defaultDomainId, onImported, onApplyDraft, parseResponse, handleClose]);

  const toggleCandidate = useCallback((tempId: string) => {
    setSelectedTempIds(prev =>
      prev.includes(tempId)
        ? prev.filter(id => id !== tempId)
        : [...prev, tempId]
    );
  }, []);

  const renderInputStep = () => (
    <div className="flex flex-col h-full gap-4">
      <Card size="small" title="配置">
        <Space direction="vertical" className="w-full">
          <div>
            <Text strong className="block mb-2">来源类型</Text>
            <Radio.Group value={sourceType} onChange={e => setSourceType(e.target.value)}>
              <Radio.Button value="PASTE_MD">粘贴文本</Radio.Button>
              <Radio.Button value="FILE_MD"><UploadOutlined /> 上传文件</Radio.Button>
            </Radio.Group>
          </div>
          <div>
            <Text strong className="block mb-2">解析模式</Text>
            <Radio.Group value={mode} onChange={e => setMode(e.target.value)}>
              <Radio value="split_by_h2">按 heading 2 切分</Radio>
              <Radio value="single_scene">整体作为一个场景</Radio>
            </Radio.Group>
          </div>
        </Space>
      </Card>

      <div className="flex-1 flex flex-col">
        <Text strong className="mb-2">文档内容</Text>
        {sourceType === 'PASTE_MD' ? (
          <>
            <TextArea
              className="flex-1 font-mono text-sm"
              placeholder="在此粘贴 Markdown 或纯文本内容..."
              value={rawText}
              onChange={e => setRawText(e.target.value)}
              style={{ resize: 'none' }}
            />
            <div className="text-right text-xs text-gray-400 mt-1">
              {rawText.length.toLocaleString()} / {maxBytes.toLocaleString()} 字节
            </div>
          </>
        ) : (
          <Upload.Dragger
            accept=".md,.markdown,.txt"
            beforeUpload={handleFile}
            maxCount={1}
            showUploadList={false}
            className="flex-1"
          >
            <p className="ant-upload-drag-icon">
              <UploadOutlined />
            </p>
            <p className="ant-upload-text">拖拽或点击上传 Markdown / TXT 文件</p>
            <p className="ant-upload-hint text-xs text-gray-500">
              文件大小不超过 {Math.floor(maxBytes / 1024)} KB
            </p>
            {uploadedFileName && (
              <Tag color="blue" className="mt-2">{uploadedFileName}</Tag>
            )}
          </Upload.Dragger>
        )}
      </div>
    </div>
  );

  const renderPreviewStep = () => (
    <div className="flex flex-col gap-4">
      {parseResponse && (
        <Alert
          message="解析完成"
          description={
            <Space>
              <span>发现 {parseResponse.sceneCandidates.length} 个候选场景</span>
              {/* Short parse stats could go here */}
            </Space>
          }
          type="success"
          showIcon
        />
      )}

      <div className="flex flex-col gap-3">
        {parseResponse?.sceneCandidates.map((candidate: SceneCandidate) => {
          const selected = selectedTempIds.includes(candidate.tempId);
          return (
            <SceneCandidateCard
              key={candidate.tempId}
              candidate={candidate}
              selected={selected}
              onToggle={() => toggleCandidate(candidate.tempId)}
            />
          );
        })}
      </div>
    </div>
  );

  const renderSuccess = () => (
    <div className="flex flex-col items-center justify-center py-12">
      <CheckCircleOutlined className="text-6xl text-green-500 mb-4" />
      <Title level={3}>导入完成</Title>
      <Text>已成功创建 {commitResponse?.createdScenes.length} 个场景草稿</Text>
      <div className="mt-6 flex flex-col items-center gap-2">
        {commitResponse?.createdScenes.map(scene => (
          <Button key={scene.sceneCode} type="link" href={`/scenes/${scene.sceneCode}/edit`}>
            {scene.sceneCode}
          </Button>
        ))}
      </div>
      <Button type="primary" className="mt-8" onClick={handleClose}>
        完成
      </Button>
    </div>
  );

  const footerButtons = () => {
    if (status === 'done') return null;

    return (
      <div className="flex justify-end gap-3">
        {currentStep === 0 && (
          <>
            <Button onClick={handleClose}>取消</Button>
            <Button
              type="primary"
              onClick={handleParse}
              loading={status === 'parsing'}
              disabled={!rawText.trim()}
            >
              下一步：解析预览
            </Button>
          </>
        )}
        {currentStep === 1 && (
          <>
            <Button onClick={() => setCurrentStep(0)} disabled={status === 'committing'}>
              上一步
            </Button>
            <Button
              type="primary"
              onClick={handleCommit}
              loading={status === 'committing'}
              disabled={selectedTempIds.length === 0}
            >
              {onApplyDraft ? '应用到表单' : `确认导入 (${selectedTempIds.length})`}
            </Button>
          </>
        )}
      </div>
    );
  };

  return (
    <Drawer
      title="导入中心"
      placement="right"
      width={720}
      onClose={handleClose}
      open={open}
      footer={footerButtons()}
      maskClosable={false}
    >
      <Steps
        current={currentStep}
        status={status === 'error' ? 'error' : 'process'}
        className="mb-8"
        items={[
          { title: '输入内容', icon: <FileTextOutlined /> },
          { title: '解析预览', icon: <AppstoreOutlined /> },
          { title: '完成', icon: <CheckCircleOutlined /> }
        ]}
      />

      {error && <Alert message={error} type="error" showIcon closable className="mb-4" onClose={() => setError(null)} />}

      {status === 'done'
        ? renderSuccess()
        : (
          <div className="h-[calc(100vh-250px)] overflow-y-auto">
            {currentStep === 0 ? renderInputStep() : renderPreviewStep()}
          </div>
        )
      }
    </Drawer>
  );
};

export default ImportDrawer;
