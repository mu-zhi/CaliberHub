import { PropsWithChildren } from 'react'

export function PageContainer({ children }: PropsWithChildren) {
    return (
        <div
            style={{
                padding: 24,
                background: 'var(--ant-color-bg-layout, #f5f5f5)',
                minHeight: 'calc(100vh - 64px)',
            }}
        >
            {children}
        </div>
    )
}

export default PageContainer
