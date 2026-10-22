import { useEffect } from 'react'
import VideoPlayer from '../components/video-player/VideoPlayer'

function Watch() {
  const videoId = decodeURIComponent(window.location.pathname.replace(/^\/watch\//, ''))

  useEffect(() => {
    if (!videoId) {
      window.history.replaceState({}, '', '/')
    }
  }, [videoId])

  if (!videoId) return null

  return (
    <main>
      <VideoPlayer videoId={videoId} title="Reproduciendo video" onClose={() => window.history.back()} />
    </main>
  )
}

export default Watch
