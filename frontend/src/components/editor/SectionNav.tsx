import React from 'react';
import { CheckCircleOutlined, WarningOutlined } from '@ant-design/icons';




export interface SectionItem {
    id: string;
    title: string;
    isCompleted?: boolean;
    hasError?: boolean;
}

interface SectionNavProps {
    sections: SectionItem[];
    activeSection?: string;
    onSectionClick: (id: string) => void;
}

const SectionNav: React.FC<SectionNavProps> = ({ sections, activeSection, onSectionClick }) => {
    return (
        <nav className="flex flex-col space-y-1">
            {sections.map(section => (
                <button
                    key={section.id}
                    onClick={() => onSectionClick(section.id)}
                    className={`
                        w-full text-left px-3 py-2 rounded-lg text-sm transition-all duration-150 flex items-center justify-between group
                        ${activeSection === section.id
                            ? 'bg-primary/10 text-primary font-medium shadow-sm'
                            : 'text-muted-foreground hover:bg-muted hover:text-foreground'
                        }
                    `}
                >
                    <span className="truncate mr-2">{section.title}</span>
                    {section.isCompleted && <CheckCircleOutlined className="text-success text-xs" />}
                    {section.hasError && <WarningOutlined className="text-warning text-xs" />}
                </button>
            ))}
        </nav>
    );
};

export default SectionNav;
