import { useState } from 'react'
import './sidebar.css'

type SidebarItem = 'home' | 'videos' | 'private' | 'hidden' | 'settings'

type SidebarProps = {
    activeItem?: SidebarItem
    onNavigate?: (item: SidebarItem) => void
    darkMode?: boolean
}

type IconName = 'home' | 'video' | 'lock' | 'eye' | 'settings' | 'sun' | 'moon'

function Icon({ name }: { name: IconName }) {
    if (name === 'home') {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                <polyline points="9 22 9 12 15 12 15 22" />
            </svg>
        )
    }
    if (name === 'video') {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="3.5" y="5" width="17" height="14" rx="2" />
                <path d="m9 9 5 3-5 3V9Z" />
            </svg>
        )
    }
    if (name === 'lock') {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <rect x="5" y="10" width="14" height="10" rx="2" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3" />
            </svg>
        )
    }
    if (name === 'eye') {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M2.5 12s3.2-5 9.5-5 9.5 5 9.5 5-3.2 5-9.5 5-9.5-5-9.5-5Z" />
                <circle cx="12" cy="12" r="2.5" />
            </svg>
        )
    }
    if (name === 'settings') {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M12 3.5 13.4 5l2-.2.8 1.8 1.8.8-.2 2L19.5 11l-1.7 1.5.2 2-1.8.8-.8 1.8-2-.2L12 18.5 10.5 17l-2 .2-.8-1.8-1.8-.8.2-2L4.5 12 6.2 10.5l-.2-2 1.8-.8.8-1.8 2 .2L12 3.5Z" />
                <circle cx="12" cy="12" r="2.5" />
            </svg>
        )
    }
    if (name === 'moon') {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
            </svg>
        )
    }
    if (name === 'sun') {
        return (
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="12" r="5" />
                <line x1="12" y1="1" x2="12" y2="3" />
                <line x1="12" y1="21" x2="12" y2="23" />
                <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                <line x1="1" y1="12" x2="3" y2="12" />
                <line x1="21" y1="12" x2="23" y2="12" />
                <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
            </svg>
        )
    }
    return null
}

const mainItems: Array<{ id: SidebarItem; label: string; icon: IconName }> = [
    { id: 'home', label: 'Inicio', icon: 'home' },
    { id: 'videos', label: 'Mis videos', icon: 'video' },
    { id: 'private', label: 'Privados', icon: 'lock' },
    { id: 'hidden', label: 'Ocultos', icon: 'eye' },
]

function Sidebar({ activeItem = 'home', onNavigate, darkMode = false }: SidebarProps) {
    const [collapsed, setCollapsed] = useState(true)
    const [selectedItem, setSelectedItem] = useState<SidebarItem>(activeItem)

    function navigate(item: SidebarItem) {
        setSelectedItem(item)
        onNavigate?.(item)
    }

    return (
        <aside
            className={`sidebar ${collapsed ? 'is-collapsed' : ''} ${darkMode ? 'is-dark' : 'is-light'}`}
            onMouseEnter={() => setCollapsed(false)}
            onMouseLeave={() => setCollapsed(true)}
        >
            <div className="sidebar-inner">
                <div className="sidebar-brand">
                    <span className="brand-mark">▶</span>
                    <span className="brand-copy">You<span>Beam</span></span>
                </div>

                <div className="sidebar-scrollable">
                    <div className="menu-section">
                        <span className="menu-label">Tu espacio</span>
                        <nav className="sidebar-nav">
                            {mainItems.map((item) => (
                                <button
                                    className={`sidebar-item ${selectedItem === item.id ? 'is-active' : ''}`}
                                    key={item.id}
                                    aria-current={selectedItem === item.id ? 'page' : undefined}
                                    title={collapsed ? item.label : undefined}
                                    onClick={() => navigate(item.id)}
                                >
                                    <div className="icon-wrapper">
                                        <Icon name={item.icon} />
                                    </div>
                                    <span className="item-label">{item.label}</span>
                                </button>
                            ))}
                        </nav>
                    </div>
                </div>

                <div className="sidebar-footer">
                    <button
                        className={`sidebar-item ${selectedItem === 'settings' ? 'is-active' : ''}`}
                        aria-current={selectedItem === 'settings' ? 'page' : undefined}
                        title={collapsed ? 'Configuración' : undefined}
                        onClick={() => navigate('settings')}
                    >
                        <div className="icon-wrapper">
                            <Icon name="settings" />
                        </div>
                        <span className="item-label">Configuración</span>
                    </button>
                </div>
            </div>
        </aside>
    )
}

export type { SidebarItem }
export default Sidebar