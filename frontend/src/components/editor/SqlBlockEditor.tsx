import React from 'react';
import { Card, Input, Button } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import { SqlBlock } from '../../types';

const { TextArea } = Input;

interface SqlBlockEditorProps {
    blocks: SqlBlock[];
    onChange: (newBlocks: SqlBlock[]) => void;
}

const SqlBlockEditor: React.FC<SqlBlockEditorProps> = ({ blocks, onChange }) => {
    const updateBlock = (index: number, field: keyof SqlBlock, value: string) => {
        const newBlocks = [...blocks];
        newBlocks[index] = { ...newBlocks[index], [field]: value };
        onChange(newBlocks);
    };

    const addBlock = () => {
        onChange([
            ...blocks,
            { blockId: `blk-${Date.now()}`, name: `SQL Block ${blocks.length + 1}`, sql: '' }
        ]);
    };

    const removeBlock = (index: number) => {
        onChange(blocks.filter((_, i) => i !== index));
    };

    return (
        <div className="space-y-4">
            {blocks.map((block, index) => (
                <Card
                    key={block.blockId}
                    size="small"
                    title={
                        <Input
                            value={block.name}
                            onChange={(e) => updateBlock(index, 'name', e.target.value)}
                            className="w-48 font-medium"
                            placeholder="Block Name"
                            bordered={false}
                        />
                    }
                    extra={
                        <Button
                            type="text"
                            danger
                            icon={<DeleteOutlined />}
                            onClick={() => removeBlock(index)}
                        />
                    }
                    className="bg-gray-50 border-gray-200"
                >
                    <TextArea
                        value={block.sql}
                        onChange={(e) => updateBlock(index, 'sql', e.target.value)}
                        rows={6}
                        className="font-mono text-sm bg-white"
                        placeholder="SELECT * FROM ..."
                    />
                    <Input
                        value={block.notes}
                        onChange={(e) => updateBlock(index, 'notes', e.target.value)}
                        placeholder="Notes (optional)"
                        className="mt-2 text-xs"
                        bordered={false}
                    />
                </Card>
            ))}
            <Button type="dashed" block icon={<PlusOutlined />} onClick={addBlock}>
                Add SQL Block
            </Button>
        </div>
    );
};

export default SqlBlockEditor;
