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

// Tamaño de página del catálogo; el Core admite como máximo 50
const CATALOG_PAGE_SIZE = 9

// Secciones que exigen sesión: sin ella se invita a iniciar sesión en lugar de mostrar un 401
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
      // Aunque falle la petición, el cliente deja de considerarse autenticado
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

  // El Core no devuelve el total del catálogo: si la página viene llena, se ofrece una más
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
        <div className="theme-setting">
          <div>
            <strong>Apariencia</strong>
            <span>Personaliza cómo ves tu espacio de creador.</span>
          </div>
          <button
            className={`theme-switch ${darkMode ? 'is-on' : ''}`}
            type="button"
            aria-pressed={darkMode}
            onClick={() => setDarkMode((enabled) => !enabled)}
          >
            <span />
            {darkMode ? 'Modo oscuro' : 'Modo claro'}
          </button>
        </div>
      )
    }
    if (needsLogin) {
      return (
        <div className="empty-state">
          <p>Inicia sesión para gestionar y subir tus videos.</p>
          <button className="primary-action" type="button" onClick={goToLogin}>Iniciar sesión</button>
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

  const showCatalogChrome = activeItem === 'home' && panel === 'catalog'

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
            {renderMain()}
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
        {playingVideo && (
          <VideoPlayer
            videoId={playingVideo.id}
            title={playingVideo.title}
            description={playingVideo.description}
            onClose={() => setPlayingVideo(null)}
          />
        )}
      </section>
    </main>
  )
}

export default Home
