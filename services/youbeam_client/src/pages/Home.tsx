import { useCallback, useEffect, useState } from 'react'
import ApiFeedback from '../components/api-feedback/ApiFeedback'
import BarHeader from '../components/bar-header/BarHeader'
import CardVideo from '../components/card-video/CardVideo'
import FilterBar from '../components/filter-bar/FilterBar'
import MyVideos from '../components/my-videos/MyVideos'
import PageEnumeration from '../components/page-enumeration/PageEnumeration'
import Sidebar from '../components/sidebar/Sidebar'
import StreamingPlayer from '../components/streaming-player/StreamingPlayer'
import UploadVideo from '../components/upload-video/UploadVideo'
import VideoPlayer from '../components/video-player/VideoPlayer'
import type { SidebarItem } from '../components/sidebar/Sidebar'
import { getCurrentUser, listCatalog, logout } from '../lib/api'
import type { CurrentUser } from '../lib/api'
import { formatDuration, formatRelativeDate } from '../lib/format'
import type { Video } from '../types/video'
import './Home.css'

const CATALOG_PAGE_SIZE = 9

const PRIVATE_SECTIONS: SidebarItem[] = ['videos', 'private', 'hidden']

function Home() {
  const [activeItem, setActiveItem] = useState<SidebarItem>('home')
  const [darkMode, setDarkMode] = useState(false)
  const [currentPage, setCurrentPage] = useState(1)
  const [view, setView] = useState<'grid' | 'list'>('grid')
  const [playingVideo, setPlayingVideo] = useState<Video | null>(null)
  const [panel, setPanel] = useState<'catalog' | 'upload'>('catalog')
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [videos, setVideos] = useState<Video[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [search, setSearch] = useState('')

  useEffect(() => {
    let mounted = true
    void getCurrentUser()
        .then((current) => {
          if (mounted) setUser(current)
        })
        .catch(() => {
          if (mounted) setUser(null)
        })
    return () => {
      mounted = false
    }
  }, [])

  const loadCatalog = useCallback(async (page: number) => {
    setLoading(true)
    setError(null)
    try {
      setVideos(await listCatalog(page - 1, CATALOG_PAGE_SIZE))
    } catch (loadError) {
      setError(loadError)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => void loadCatalog(currentPage), 0)
    return () => window.clearTimeout(timer)
  }, [loadCatalog, currentPage])

  async function handleLogout() {
    try {
      await logout()
    } finally {
      setUser(null)
      setActiveItem('home')
      setPanel('catalog')
    }
  }

  function navigate(item: SidebarItem) {
    setActiveItem(item)
    if (item !== 'home') {
      setPanel('catalog')
    }
  }

  function goToLogin() {
    window.location.assign('/auth')
  }

  const totalItems = (currentPage - 1) * CATALOG_PAGE_SIZE + videos.length
      + (videos.length === CATALOG_PAGE_SIZE ? 1 : 0)
  const query = search.trim().toLowerCase()
  const visibleVideos = query
      ? videos.filter((video) => `${video.title} ${video.description ?? ''}`.toLowerCase().includes(query))
      : videos
  const needsLogin = user === null && (PRIVATE_SECTIONS.includes(activeItem) || panel === 'upload')

  function renderMain() {
    if (activeItem === 'settings') {
      return (
          <div className="settings-container">
            <header className="settings-header">
              <h1>Configuración</h1>
              <p>Administra tu perfil y preferencias de visualización</p>
            </header>

            <div className="settings-list">
              <div className="settings-card">
                <div className="profile-info-group">
                  <div className="profile-avatar">
                    {user ? (user.displayName?.charAt(0) || user.username.charAt(0)).toUpperCase() : '?'}
                  </div>
                  <div className="profile-details">
                    <strong>{user ? (user.displayName || user.username) : 'Sesión de invitado'}</strong>
                    <span>{user ? 'Cuenta activa' : 'Inicia sesión para gestionar tus videos'}</span>
                  </div>
                </div>
                {user ? (
                    <button className="settings-action-btn is-logout" type="button" onClick={() => void handleLogout()}>
                      Cerrar sesión
                    </button>
                ) : (
                    <button className="settings-action-btn is-login" type="button" onClick={goToLogin}>
                      Iniciar sesión
                    </button>
                )}
              </div>

              <div className="settings-card">
                <div className="settings-text-group">
                  <strong>Apariencia</strong>
                  <span>Cambia entre el modo claro y oscuro</span>
                </div>
                <button
                    className={`modern-toggle ${darkMode ? 'is-active' : ''}`}
                    type="button"
                    aria-pressed={darkMode}
                    onClick={() => setDarkMode((enabled) => !enabled)}
                >
                  <div className="toggle-track"><span className="toggle-knob" /></div>
                  <span className="toggle-label">{darkMode ? 'Oscuro' : 'Claro'}</span>
                </button>
              </div>
            </div>
          </div>
      )
    }

    if (needsLogin) {
      return (
          <div className="home-premium-state-container">
            <div className="home-premium-state">
              <div className="home-state-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
                  <path d="M12 15v2m-6 4h12a2 2 0 0 0 2-2v-6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2zm10-10V7a4 4 0 0 0-8 0v4h8z"/>
                </svg>
              </div>
              <h2 className="home-state-title">Acceso requerido</h2>
              <p className="home-state-desc">Para acceder a esta sección y subir o gestionar tu contenido, necesitas iniciar sesión en tu cuenta.</p>
              <button className="home-btn-corporate" type="button" onClick={goToLogin}>
                Iniciar sesión
              </button>
            </div>
          </div>
      )
    }

    if (PRIVATE_SECTIONS.includes(activeItem)) {
      return (
          <MyVideos
              key={activeItem}
              visibilityFilter={activeItem === 'private' ? 'private' : activeItem === 'hidden' ? 'hidden' : undefined}
              onPlay={setPlayingVideo}
              onUpload={() => {
                setActiveItem('home')
                setPanel('upload')
              }}
          />
      )
    }
    if (activeItem === 'streaming') {
      return <StreamingPlayer />
    }
    if (panel === 'upload') {
      return (
          <UploadVideo
              onCancel={() => setPanel('catalog')}
              onComplete={() => {
                setPanel('catalog')
                setActiveItem('videos')
                void loadCatalog(currentPage)
              }}
          />
      )
    }
    if (error !== null) {
      return <ApiFeedback error={error} onRetry={() => void loadCatalog(currentPage)} />
    }
    if (loading) {
      return <p className="empty-state">Cargando catálogo...</p>
    }
    if (visibleVideos.length === 0) {
      return <p className="empty-state">{query ? 'Ningún video coincide con la búsqueda' : 'Aun no hay elementos disponibles'}</p>
    }
    return (
        <div className={`video-card-list video-card-list--${view}`}>
          {visibleVideos.map((video) => (
              <CardVideo
                  key={video.id}
                  videoId={video.id}
                  title={video.title}
                  description={video.description ?? undefined}
                  duration={formatDuration(video.durationSeconds)}
                  publishedAt={formatRelativeDate(video.publishedAt ?? video.createdAt)}
                  variant={view}
                  onPlay={() => setPlayingVideo(video)}
              />
          ))}
        </div>
    )
  }

  const showCatalogChrome = activeItem === 'home' && panel === 'catalog' && playingVideo === null

  return (
      <main className="home-page">
        <Sidebar activeItem={activeItem} onNavigate={navigate} darkMode={darkMode} />
        <section className={`home-content ${darkMode ? 'is-dark' : ''}`}>
          {showCatalogChrome && (
              <>
                <BarHeader
                    isAuthenticated={user !== null}
                    userName={user?.displayName || user?.username}
                    onSearch={setSearch}
                    onLogin={goToLogin}
                    onLogout={() => void handleLogout()}
                    onUpload={() => setPanel('upload')}
                />
                <FilterBar view={view} onViewChange={setView} />
              </>
          )}
          <div className="home-scroll">
            <div className={`home-main ${activeItem === 'home' ? 'home-main--home' : ''}`}>
              {playingVideo ? (
                  <VideoPlayer
                      videoId={playingVideo.id}
                      title={playingVideo.title}
                      description={playingVideo.description}
                      onClose={() => setPlayingVideo(null)}
                  />
              ) : renderMain()}
            </div>
          </div>
          {showCatalogChrome && (
              <PageEnumeration
                  totalItems={totalItems}
                  itemsPerPage={CATALOG_PAGE_SIZE}
                  currentPage={currentPage}
                  onPageChange={setCurrentPage}
              />
          )}
        </section>
      </main>
  )
}

export default Home