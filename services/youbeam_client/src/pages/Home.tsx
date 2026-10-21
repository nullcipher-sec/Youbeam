import { useState } from 'react'
import Sidebar from '../components/sidebar/Sidebar'
import type { SidebarItem } from '../components/sidebar/Sidebar'
import './Home.css'

function Home() {
  const [activeItem, setActiveItem] = useState<SidebarItem>('videos')
  const [darkMode, setDarkMode] = useState(false)

  return (
    <main className="home-page">
      <Sidebar activeItem={activeItem} onNavigate={setActiveItem} darkMode={darkMode} />
      <section className={`home-content ${darkMode ? 'is-dark' : ''}`}>
        {activeItem === 'settings' ? (
          <>
            <p className="home-eyebrow">Preferencias de YouBeam</p>
            <h1>Configuración</h1>
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
          </>
        ) : (
          <>
            <p className="home-eyebrow">Tu espacio de creador</p>
            <h1>{activeItem === 'videos' ? 'Mis videos' : 'Mis contenidos'}</h1>
            <p className="home-description">
              Gestiona tu contenido y descubre nuevas formas de conectar con tu comunidad.
            </p>
          </>
        )}
      </section>
    </main>
  )
}

export default Home
