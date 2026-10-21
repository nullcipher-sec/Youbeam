import { useEffect, useState } from 'react'
import BarHeader from '../components/bar-header/BarHeader'
import CardVideo from '../components/card-video/CardVideo'
import FilterBar from '../components/filter-bar/FilterBar'
import MyVideos from '../components/my-videos/MyVideos'
import PageEnumeration from '../components/page-enumeration/PageEnumeration'
import Sidebar from '../components/sidebar/Sidebar'
import UploadVideo from '../components/upload-video/UploadVideo'
import VideoPlayer from '../components/video-player/VideoPlayer'
import type { SidebarItem } from '../components/sidebar/Sidebar'
import { getCurrentUser } from '../lib/api'
import './Home.css'

const videoCatalog = [
  {
    title: 'Cómo diseñar interfaces limpias y minimalistas sin...',
    creator: 'Estudio Vector',
    views: '120K',
    publishedAt: '2 días',
    duration: '14:20',
    thumbnail: 'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=estudio-vector',
    badge: '4K UHD',
    verified: true,
  },
  {
    title: 'De habitantes a dueños: Justicia histórica y 5.000 nuevos propietarios en el Orinoco',
    creator: 'Así Mismo Es... Podcast',
    views: '492',
    publishedAt: '2 sem.',
    duration: '32:16',
    thumbnail: 'https://images.unsplash.com/photo-1478737270239-2f02b77fc618?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=asimismo-podcast',
    description: 'Un análisis profundo sobre el impacto histórico, social y económico de la entrega de más de 5.000 títulos.',
  },
  {
    title: 'Higiene y seguridad industrial: conceptos fundamentales',
    creator: 'Andrés Arrias',
    views: '1.2K',
    publishedAt: '1 mes',
    duration: '18:42',
    thumbnail: 'https://images.unsplash.com/photo-1581094794329-c8112a89af12?auto=format&fit=crop&w=900&q=80',
    badge: 'Nuevo',
  },
  {
    title: 'Productividad real: organiza tu semana sin complicaciones',
    creator: 'Enfoque Diario',
    views: '8.4K',
    publishedAt: '3 días',
    duration: '09:18',
    thumbnail: 'https://images.unsplash.com/photo-1499750310107-5fef28a66643?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=enfoque-diario',
    badge: 'Tendencia',
  },
  {
    title: 'Fotografía 4K: cómo aprovechar la luz natural',
    creator: 'Luz y Lente',
    views: '16K',
    publishedAt: '5 días',
    duration: '12:06',
    thumbnail: 'https://images.unsplash.com/photo-1452780212940-6f5c0d14d848?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=luz-y-lente',
    badge: '4K UHD',
    verified: true,
  },
  {
    title: 'Construye tu primer setup gaming con poco presupuesto',
    creator: 'Zona Gamer',
    views: '24K',
    publishedAt: '1 sem.',
    duration: '15:40',
    thumbnail: 'https://images.unsplash.com/photo-1593305841991-05c297ba4575?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=zona-gamer',
    badge: 'Gaming',
  },
  {
    title: 'Música para concentrarte y trabajar mejor',
    creator: 'Frecuencia',
    views: '31K',
    publishedAt: '1 sem.',
    duration: '28:15',
    thumbnail: 'https://images.unsplash.com/photo-1511379938547-c1f69419868d?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=frecuencia',
  },
  {
    title: 'Diseño UI/UX: errores que afectan la experiencia',
    creator: 'Producto Claro',
    views: '6.8K',
    publishedAt: '2 sem.',
    duration: '11:32',
    thumbnail: 'https://images.unsplash.com/photo-1559028012-481c04fa702d?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=producto-claro',
    verified: true,
  },
  {
    title: 'Tutorial de edición de video para principiantes',
    creator: 'Corte Final',
    views: '19K',
    publishedAt: '3 sem.',
    duration: '21:08',
    thumbnail: 'https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=corte-final',
    badge: 'Tutorial',
  },
  {
    title: 'Ideas sencillas para transformar tu espacio de trabajo',
    creator: 'Casa Creativa',
    views: '12K',
    publishedAt: '1 mes',
    duration: '08:54',
    thumbnail: 'https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=casa-creativa',
  },
  {
    title: 'Cómo presentar tus proyectos con más claridad',
    creator: 'Punto de Vista',
    views: '4.2K',
    publishedAt: '1 mes',
    duration: '13:27',
    thumbnail: 'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=punto-de-vista',
  },
  {
    title: 'Guía rápida para comenzar a crear contenido',
    creator: 'Crea Hoy',
    views: '27K',
    publishedAt: '2 meses',
    duration: '10:44',
    thumbnail: 'https://images.unsplash.com/photo-1492724441997-5dc865305da7?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=crea-hoy',
    badge: 'Popular',
  },
  {
    title: 'Aprende a contar historias con una cámara',
    creator: 'Historias Visuales',
    views: '9.7K',
    publishedAt: '2 meses',
    duration: '16:20',
    thumbnail: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=historias-visuales',
    badge: 'Nuevo',
  },
  {
    title: 'Herramientas digitales para estudiar mejor',
    creator: 'Aprende Fácil',
    views: '14K',
    publishedAt: '2 meses',
    duration: '07:36',
    thumbnail: 'https://images.unsplash.com/photo-1523240795612-9a054b0db644?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=aprende-facil',
  },
  {
    title: 'Introducción al modelado 3D para creativos',
    creator: 'Mundo 3D',
    views: '7.3K',
    publishedAt: '3 meses',
    duration: '19:12',
    thumbnail: 'https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=mundo-3d',
    badge: 'Tutorial',
  },
  {
    title: 'Rutinas cortas para cuidar tu bienestar',
    creator: 'Pausa Activa',
    views: '22K',
    publishedAt: '3 meses',
    duration: '10:05',
    thumbnail: 'https://images.unsplash.com/photo-1518611012118-696072aa579a?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=pausa-activa',
  },
  {
    title: 'Los fundamentos del color en el diseño',
    creator: 'Color Studio',
    views: '11K',
    publishedAt: '4 meses',
    duration: '14:48',
    thumbnail: 'https://images.unsplash.com/photo-1549490349-8643362247b5?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=color-studio',
    badge: 'Tutorial',
  },
  {
    title: 'Viaja ligero: consejos para organizar tu equipaje',
    creator: 'Ruta Abierta',
    views: '18K',
    publishedAt: '4 meses',
    duration: '09:52',
    thumbnail: 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=ruta-abierta',
  },
  {
    title: 'Cómo mejorar el audio de tus grabaciones',
    creator: 'Sonido Pro',
    views: '13K',
    publishedAt: '5 meses',
    duration: '17:03',
    thumbnail: 'https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=sonido-pro',
    badge: '4K UHD',
    verified: true,
  },
  {
    title: 'Cocina práctica para todos los días',
    creator: 'Sabor Casero',
    views: '35K',
    publishedAt: '5 meses',
    duration: '12:29',
    thumbnail: 'https://images.unsplash.com/photo-1556910103-1c02745aae4d?auto=format&fit=crop&w=900&q=80',
    avatar: 'https://i.pravatar.cc/100?u=sabor-casero',
    badge: 'Popular',
  },
]

function getRandomVideos() {
  return [...videoCatalog].sort(() => Math.random() - 0.5)
}

function Home() {
  const [activeItem, setActiveItem] = useState<SidebarItem>('home')
  const [darkMode, setDarkMode] = useState(false)
  const [currentPage, setCurrentPage] = useState(1)
  const [view, setView] = useState<'grid' | 'list'>('grid')
  const [playingVideoId, setPlayingVideoId] = useState<string | null>(null)
  const [panel, setPanel] = useState<'catalog' | 'upload'>('catalog')
  const [isAuthenticated, setIsAuthenticated] = useState(false)

  const [videos] = useState(getRandomVideos)

  useEffect(() => {
    let mounted = true
    void getCurrentUser()
      .then((user) => {
        if (mounted) {
          setIsAuthenticated(user !== null)
        }
      })
      .catch(() => {
        if (mounted) {
          setIsAuthenticated(false)
        }
      })

    return () => {
      mounted = false
    }
  }, [])

  const hasContent = activeItem === 'home' && videos.length > 0

  function navigate(item: SidebarItem) {
    setActiveItem(item)
    if (item !== 'home') {
      setPanel('catalog')
    }
  }

  return (
    <main className="home-page">
      <Sidebar activeItem={activeItem} onNavigate={navigate} darkMode={darkMode} />
      <section className={`home-content ${darkMode ? 'is-dark' : ''}`}>
        {activeItem === 'home' && panel === 'catalog' && (
          <>
            <BarHeader
              isAuthenticated={isAuthenticated}
              onLogin={() => window.location.assign('/auth')}
              onUpload={() => setPanel('upload')}
            />
            <FilterBar view={view} onViewChange={setView} />
          </>
        )}
        <div className="home-scroll">
          <div className={`home-main ${activeItem === 'home' ? 'home-main--home' : ''}`}>
            {activeItem === 'settings' ? (
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
            ) : activeItem === 'videos' ? (
              <MyVideos onUpload={() => {
                setActiveItem('home')
                setPanel('upload')
              }} />
            ) : panel === 'upload' ? (
              <UploadVideo onCancel={() => setPanel('catalog')} onComplete={() => {
                setPanel('catalog')
                setActiveItem('videos')
              }} />
            ) : hasContent ? (
              <div className={`video-card-list video-card-list--${view}`}>
                {videos.map((video, index) => (
                  <CardVideo
                    key={video.title}
                    {...video}
                    videoId={`video-${index + 1}`}
                    variant={view}
                    onPlay={setPlayingVideoId}
                  />
                ))}
              </div>
            ) : (
              <p className="empty-state">Aun no hay elementos disponibles</p>
            )}
          </div>
        </div>
        {activeItem === 'home' && panel === 'catalog' && (
          <PageEnumeration
            totalItems={128}
            itemsPerPage={9}
            currentPage={currentPage}
            onPageChange={setCurrentPage}
          />
        )}
        {playingVideoId && (
          <VideoPlayer
            videoId={playingVideoId}
            title={videos.find((_, index) => `video-${index + 1}` === playingVideoId)?.title ?? 'Video'}
            onClose={() => setPlayingVideoId(null)}
          />
        )}
      </section>
    </main>
  )
}

export default Home
