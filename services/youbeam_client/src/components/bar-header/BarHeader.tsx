import { useState, useRef, useEffect } from 'react'
import './bar-header.css'

type BarHeaderProps = {
    onSearch?: (value: string) => void
    onUpload?: () => void
    isAuthenticated?: boolean
    userName?: string
    onLogin?: () => void
    onLogout?: () => void
}

function SearchIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="10.8" cy="10.8" r="6.8" />
            <path d="m16 16 5 5" />
        </svg>
    )
}

function UploadIcon() {
    return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 16V4" />
            <path d="m7.5 8.5 4.5-4.5 4.5 4.5" />
            <path d="M4 14.5V19a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4.5" />
        </svg>
    )
}

function BarHeader({ onSearch, onUpload, isAuthenticated = false, userName = '', onLogin, onLogout }: BarHeaderProps) {
    const [isMenuOpen, setIsMenuOpen] = useState(false)
    const menuRef = useRef<HTMLDivElement>(null)

    useEffect(() => {
        function handleClickOutside(event: MouseEvent) {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setIsMenuOpen(false)
            }
        }

        document.addEventListener('mousedown', handleClickOutside)

        return () => {
            document.removeEventListener('mousedown', handleClickOutside)
        }
    }, [])

    function handleSearch(event: React.ChangeEvent<HTMLInputElement>) {
        onSearch?.(event.target.value)
    }

    return (
        <header className="bar-header">
            <label className="bar-header-search">
                <SearchIcon />
                <input
                    type="search"
                    placeholder="Buscar videos..."
                    aria-label="Buscar videos"
                    onChange={handleSearch}
                />
            </label>

            <div className="bar-header-actions">
                {isAuthenticated ? (
                    <>
                        <button className="bar-header-upload" type="button" onClick={onUpload}>
                            <UploadIcon />
                            <span>Subir</span>
                        </button>

                        <div className="user-menu-container" ref={menuRef}>
                    <button
                        className="avatar-button"
                        onClick={() => setIsMenuOpen(!isMenuOpen)}
                        aria-expanded={isMenuOpen}
                    >
                        {/* Inicial del usuario en lugar de un avatar externo: así no se filtra la visita a terceros */}
                        <span className="bar-header-avatar bar-header-initial" aria-label={`Perfil de ${userName}`}>
                            {userName.charAt(0).toUpperCase() || '?'}
                        </span>
                    </button>

                            {isMenuOpen && (
                                <div className="user-dropdown">
                                    <span className="user-dropdown-item user-dropdown-name">{userName}</span>
                                    <div className="user-dropdown-divider"></div>
                                    <button className="user-dropdown-item is-danger" type="button" onClick={() => {
                                        setIsMenuOpen(false)
                                        onLogout?.()
                                    }}>Cerrar sesión</button>
                                </div>
                            )}
                        </div>
                    </>
                ) : (
                    <button className="bar-header-login" type="button" onClick={onLogin}>
                        Iniciar sesión
                    </button>
                )}
            </div>
        </header>
    )
}

export default BarHeader