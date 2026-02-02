import { useLocation, useNavigate } from 'react-router-dom'
import { Menu } from 'antd'
import {
    DatabaseOutlined,
    FileTextOutlined,
    SettingOutlined,
    CloudUploadOutlined,
} from '@ant-design/icons'

const menuItems = [
    {
        key: '/explore',
        icon: <FileTextOutlined />,
        label: '探索',
    },
    {
        key: '/workbench',
        icon: <DatabaseOutlined />,
        label: '编辑台',
    },
    {
        key: '/imports',
        icon: <CloudUploadOutlined />,
        label: '导入',
    },
    {
        key: 'governance',
        icon: <SettingOutlined />,
        label: '治理',
        children: [
            { key: '/governance/domains', label: '领域' },
            { key: '/governance/lint', label: '校验门禁' },
        ],
    },
]

export function SideNav() {
    const navigate = useNavigate()
    const location = useLocation()

    const getSelectedKey = () => {
        const path = location.pathname
        if (path.startsWith('/explore')) return '/explore'
        if (path.startsWith('/workbench')) return '/workbench'
        if (path.startsWith('/imports')) return '/imports'
        if (path.startsWith('/governance/domains')) return '/governance/domains'
        if (path.startsWith('/governance/lint')) return '/governance/lint'
        return path
    }

    const defaultOpenKeys = location.pathname.startsWith('/governance') ? ['governance'] : []

    return (
        <Menu
            mode="inline"
            selectedKeys={[getSelectedKey()]}
            defaultOpenKeys={defaultOpenKeys}
            style={{ height: '100%', borderRight: 0 }}
            items={menuItems}
            onClick={({ key }) => {
                if (key === 'governance') return
                navigate(key)
            }}
        />
    )
}

export default SideNav
