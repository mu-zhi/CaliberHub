import React from 'react';
import { Input } from 'antd';
import { SearchOutlined } from '@ant-design/icons';

interface GlobalSearchBarProps {
    placeholder?: string;
    onSearch?: (value: string) => void;
    onChange?: (value: string) => void;
}

const GlobalSearchBar: React.FC<GlobalSearchBarProps> = ({ placeholder = "搜索场景、领域、导入任务...", onSearch, onChange }) => {
    return (
        <div className="w-full max-w-[600px] mx-auto">
            <Input
                size="large"
                placeholder={placeholder}
                prefix={<SearchOutlined className="text-gray-400" />}
                className="rounded-full border-gray-200 shadow-sm hover:border-blue-400 focus:border-blue-500"
                onPressEnter={(e) => onSearch?.(e.currentTarget.value)}
                onChange={(e) => onChange?.(e.currentTarget.value)}
            />
        </div>
    );
};

export default GlobalSearchBar;
