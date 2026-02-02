import { Navigate, createBrowserRouter } from 'react-router-dom'
import { Suspense, lazy } from 'react'
import { Spin } from 'antd'
import AppShell from './layout/AppShell'
import GovernanceLayout from '@/pages/governance/GovernanceLayout'
import ErrorBoundary from '@/components/ErrorBoundary'

const ExplorePage = lazy(() => import('@/pages/explore/ExplorePage'))
const WorkbenchPage = lazy(() => import('@/pages/workbench/WorkbenchPage'))
const ImportsPage = lazy(() => import('@/pages/imports/ImportsPage'))
const SceneDetailPage = lazy(() => import('@/pages/scenes/SceneDetailPage'))
const SceneEditPage = lazy(() => import('@/pages/scenes/SceneEditPage'))
const DomainsPage = lazy(() => import('@/pages/governance/domains/DomainsPage'))
const LintSettingsPage = lazy(() => import('@/pages/governance/lint/LintSettingsPage'))
const SettingsPage = lazy(() => import('@/pages/settings/SettingsPage'))

const withBoundary = (node: React.ReactNode) => (
    <ErrorBoundary fallback={<div style={{ padding: 24 }}><Spin /></div>}>
        <Suspense fallback={<div style={{ padding: 24 }}><Spin /></div>}>{node}</Suspense>
    </ErrorBoundary>
)

export const router = createBrowserRouter([
    {
        path: '/',
        element: <AppShell />,
        children: [
            { index: true, element: <Navigate to="/explore" replace /> },

            { path: 'explore', element: withBoundary(<ExplorePage />) },
            { path: 'workbench', element: withBoundary(<WorkbenchPage />) },
            { path: 'imports', element: withBoundary(<ImportsPage />) },

            { path: 'scenes/:id', element: withBoundary(<SceneDetailPage />) },
            { path: 'scenes/:id/edit', element: withBoundary(<SceneEditPage />) },

            {
                path: 'governance',
                element: <GovernanceLayout />,
                children: [
                    { index: true, element: <Navigate to="/governance/domains" replace /> },
                    { path: 'domains', element: withBoundary(<DomainsPage />) },
                    { path: 'lint', element: withBoundary(<LintSettingsPage />) },
                ],
            },

            // backward compat
            { path: 'domains', element: <Navigate to="/governance/domains" replace /> },
            { path: 'settings', element: withBoundary(<SettingsPage />) },

            { path: '*', element: <Navigate to="/explore" replace /> },
        ],
    },
])

export default router
