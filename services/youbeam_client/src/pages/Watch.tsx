import { useEffect, useState } from 'react'
import ApiFeedback from '../components/api-feedback/ApiFeedback'
import VideoPlayer from '../components/video-player/VideoPlayer'
import { getVideo } from '../lib/api'
import type { Video } from '../types/video'

/**
 * Página de un video (/watch/{id}). Es el destino de los enlaces compartidos: los videos ocultos
 * solo se pueden abrir así. El Core decide si el usuario puede verlo (404 si no).
 */
function Watch() {
  const videoId = decodeURIComponent(window.location.pathname.replace(/^\/watch\//, ''))
  const [video, setVideo] = useState<Video | null>(null)
  const [error, setError] = useState<unknown>(null)

  useEffect(() => {
    if (!videoId) {
      window.history.replaceState({}, '', '/')
      return
    }
    let cancelled = false
    getVideo(videoId)
      .then((loaded) => {
        if (!cancelled) setVideo(loaded)
      })
      .catch((loadError: unknown) => {
        if (!cancelled) setError(loadError)
      })
    return () => {
      cancelled = true
    }
  }, [videoId])

  if (!videoId) return null

  function close() {
    window.location.assign('/')
  }

  if (error !== null) {
    return (
      <main className="watch-page">
        <ApiFeedback error={error} />
        <button className="primary-action" type="button" onClick={close}>Volver al inicio</button>
      </main>
    )
  }

  if (!video) {
    return <main className="watch-page"><p className="panel-status">Cargando video...</p></main>
  }

  return (
    <main className="watch-page">
      {video.status === 'ready' ? (
        <VideoPlayer videoId={video.id} title={video.title} description={video.description} onClose={close} />
      ) : (
        <>
          <p className="panel-status">Este video todavía se está procesando.</p>
          <button className="primary-action" type="button" onClick={close}>Volver al inicio</button>
        </>
      )}
    </main>
  )
}

export default Watch
