import React, { ReactNode, useState } from 'react';
import { Button, Drawer } from 'antd';

interface EditorLayoutProps {
    leftNav: ReactNode;
    children: ReactNode; // Main content
    rightPanel: ReactNode;
}

const EditorLayout: React.FC<EditorLayoutProps> = ({ leftNav, children, rightPanel }) => {
    const [navOpen, setNavOpen] = useState(false)
    const [panelOpen, setPanelOpen] = useState(false)

    return (
        <div className="flex h-full overflow-hidden bg-background">
            {/* Left Stick Nav */}
            <aside className="hidden lg:block w-56 flex-shrink-0 border-r border-border bg-card overflow-y-auto">
                <div className="p-4 sticky top-0 bg-card z-10">
                    <h3 className="text-muted-foreground text-xs font-bold uppercase tracking-wider mb-2">导航</h3>
                </div>
                <div className="px-2 pb-4">
                    {leftNav}
                </div>
            </aside>

            {/* Main Content (Center) */}
            <main className="flex-1 overflow-y-auto p-6 scroll-smooth">
                <div className="lg:hidden flex gap-2 mb-4 sticky top-0 z-10 bg-background/90 backdrop-blur-sm py-2">
                    <Button size="small" onClick={() => setNavOpen(true)}>Sections</Button>
                    <Button size="small" onClick={() => setPanelOpen(true)}>Insights</Button>
                </div>
                <div className="max-w-[800px] mx-auto space-y-6 pb-20">
                    {children}
                </div>
            </main>

            {/* Right Panel */}
            <aside className="hidden lg:block w-[320px] flex-shrink-0 border-l border-border bg-card overflow-y-auto">
                {rightPanel}
            </aside>

            <Drawer
                title="Sections"
                open={navOpen}
                onClose={() => setNavOpen(false)}
                placement="left"
                width={260}
                destroyOnClose
            >
                {leftNav}
            </Drawer>

            <Drawer
                title="Insights"
                open={panelOpen}
                onClose={() => setPanelOpen(false)}
                placement="right"
                width={340}
                destroyOnClose
            >
                {rightPanel}
            </Drawer>
        </div>
    );
};

export default EditorLayout;
