import { Layout, theme } from 'antd'
import { Outlet } from 'react-router-dom'
import TopBar from './TopBar'
import SideNav from './SideNav'
import PageContainer from './PageContainer'

const { Sider, Content } = Layout

export function AppShell() {
    const { token } = theme.useToken()

    return (
        <Layout style={{ minHeight: '100vh' }}>
            <TopBar />
            <Layout>
                <Sider
                    width={200}
                    style={{
                        background: token.colorBgContainer,
                        borderRight: `1px solid ${token.colorBorderSecondary}`,
                    }}
                >
                    <SideNav />
                </Sider>
                <Content>
                    <PageContainer>
                        <Outlet />
                    </PageContainer>
                </Content>
            </Layout>
        </Layout>
    )
}

export default AppShell
