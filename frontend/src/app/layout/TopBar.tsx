import { Layout, Typography, theme } from 'antd'

const { Header } = Layout
const { Title } = Typography

export function TopBar() {
    const { token } = theme.useToken()
    return (
        <Header
            style={{
                display: 'flex',
                alignItems: 'center',
                padding: '0 24px',
                background: token.colorBgContainer,
                borderBottom: `1px solid ${token.colorBorderSecondary}`,
            }}
        >
            <Title level={4} style={{ margin: 0, color: token.colorPrimary }}>
                CaliberHub
            </Title>
            <span style={{ marginLeft: 12, color: token.colorTextSecondary, fontSize: 14 }}>
                口径知识资产平台
            </span>
        </Header>
    )
}

export default TopBar
