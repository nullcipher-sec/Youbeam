import { useCallback, useEffect, useState } from 'react'
import { deleteVideo, listMyVideos, updateVideo } from '../../lib/api'
import type { Video, VideoVisibility } from '../../types/video'
import ApiFeedback from '../api-feedback/ApiFeedback'
import './my-videos.css'

type MyVideosProps = {
  onUpload: () => void
}

function MyVideos({ onUpload }: MyVideosProps) {
  const [videos, setVideos] = useState<Video[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [savingId, setSavingId] = useState<string | null>(null)

  const loadVideos = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setVideos(await listMyVideos())
    } catch (loadError) {
      setError(loadError)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => void loadVideos(), 0)
    return () => window.clearTimeout(timer)
  }, [loadVideos])

  async function saveVideo(video: Video) {
    setSavingId(video.id)
    try {
      const saved = await updateVideo(video.id, {
        title: video.title,
        description: video.description,
        visibility: video.visibility,
      })
      setVideos((current) => current.map((item) => item.id === saved.id ? saved : item))
      setEditingId(null)
    } catch (saveError) {
      setError(saveError)
    } finally {
      setSavingId(null)
    }
  }

  async function removeVideo(id: string) {
    if (!window.confirm('¿Quieres borrar este video? Esta acción no se puede deshacer.')) return
    try {
      await deleteVideo(id)
      setVideos((current) => current.filter((video) => video.id !== id))
    } catch (deleteError) {
      setError(deleteError)
    }
  }

  return (
    <section className="my-videos">
      <div className="section-heading">
        <div>
          <span className="section-kicker">Tu biblioteca</span>
          <h1>Mis videos</h1>
          <p>Administra títulos, descripciones y visibilidad de tus publicaciones.</p>
        </div>
        <button className="primary-action" type="button" onClick={onUpload}>Subir video</button>
      </div>
      {error !== null && <ApiFeedback error={error} onRetry={() => void loadVideos()} />}
      {loading ? (
        <p className="panel-status">Cargando tus videos...</p>
      ) : videos.length === 0 ? (
        <p className="panel-status">Aun no hay elementos disponibles</p>
      ) : (
        <div className="my-video-table">
          {videos.map((video) => {
            const editing = editingId === video.id
            return (
              <article className="my-video-row" key={video.id}>
                <img src={video.thumbnail} alt="" />
                <div className="my-video-info">
                  {editing ? (
                    <>
                      <input value={video.title} aria-label="Título" onChange={(event) => setVideos((current) => current.map((item) => item.id === video.id ? { ...item, title: event.target.value } : item))} />
                      <textarea value={video.description ?? ''} aria-label="Descripción" onChange={(event) => setVideos((current) => current.map((item) => item.id === video.id ? { ...item, description: event.target.value } : item))} />
                    </>
                  ) : (
                    <>
                      <h2>{video.title}</h2>
                      <p>{video.description || 'Sin descripción'}</p>
                    </>
                  )}
                  <span className={`status-chip status-chip--${video.status ?? 'ready'}`}>{video.status ?? 'ready'}</span>
                </div>
                <label className="visibility-field">
                  <span>Visibilidad</span>
                  <select disabled={!editing} value={video.visibility} onChange={(event) => setVideos((current) => current.map((item) => item.id === video.id ? { ...item, visibility: event.target.value as VideoVisibility } : item))}>
                    <option value="public">Público</option>
                    <option value="hidden">Oculto</option>
                    <option value="private">Privado</option>
                  </select>
                </label>
                <div className="my-video-actions">
                  {editing ? (
                    <button type="button" disabled={savingId === video.id} onClick={() => void saveVideo(video)}>
                      {savingId === video.id ? 'Guardando...' : 'Guardar'}
                    </button>
                  ) : (
                    <button type="button" onClick={() => setEditingId(video.id)}>Editar</button>
                  )}
                  <button className="danger-action" type="button" onClick={() => void removeVideo(video.id)}>Borrar</button>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

export default MyVideos
