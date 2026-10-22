import { useCallback, useEffect, useState } from 'react'
import { deleteVideo, listMyVideos, updateVideo, uploadVideoFile } from '../../lib/api'
import { formatDuration, statusLabels, visibilityLabels } from '../../lib/format'
import type { Video, VideoVisibility } from '../../types/video'
import ApiFeedback from '../api-feedback/ApiFeedback'
import './my-videos.css'

type MyVideosProps = {
  onUpload: () => void
  onPlay: (video: Video) => void
  // Sin filtro se muestran todos; con filtro, las secciones "Privados" y "Ocultos" de la barra lateral
  visibilityFilter?: VideoVisibility
}

const headings: Record<VideoVisibility | 'all', { title: string; description: string }> = {
  all: { title: 'Mis videos', description: 'Administra títulos, descripciones y visibilidad de tus publicaciones.' },
  private: { title: 'Videos privados', description: 'Solo tú puedes verlos y reproducirlos.' },
  hidden: { title: 'Videos ocultos', description: 'No aparecen en el catálogo; los ve quien tenga el enlace.' },
  public: { title: 'Videos públicos', description: 'Aparecen en el catálogo para cualquier persona.' },
}

function MyVideos({ onUpload, onPlay, visibilityFilter }: MyVideosProps) {
  const [videos, setVideos] = useState<Video[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<unknown>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [savingId, setSavingId] = useState<string | null>(null)
  const [uploading, setUploading] = useState<{ id: string; progress: number } | null>(null)
  const [copiedId, setCopiedId] = useState<string | null>(null)

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

  function replace(saved: Video) {
    setVideos((current) => current.map((item) => item.id === saved.id ? saved : item))
  }

  function edit(id: string, changes: Partial<Video>) {
    setVideos((current) => current.map((item) => item.id === id ? { ...item, ...changes } : item))
  }

  async function saveVideo(video: Video) {
    setSavingId(video.id)
    try {
      replace(await updateVideo(video.id, {
        title: video.title,
        description: video.description ?? '',
        visibility: video.visibility,
      }))
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

  // Para videos sin archivo o cuya subida falló: el Core admite un nuevo intento mientras no estén listos
  async function retryUpload(id: string, file: File | undefined) {
    if (!file) return
    setError(null)
    setUploading({ id, progress: 0 })
    try {
      replace(await uploadVideoFile(id, file, (progress) => setUploading({ id, progress })))
    } catch (uploadError) {
      setError(uploadError)
      void loadVideos()
    } finally {
      setUploading(null)
    }
  }

  // Los videos ocultos solo se comparten con este enlace: el UUID no se puede adivinar
  async function copyLink(id: string) {
    await navigator.clipboard.writeText(`${window.location.origin}/watch/${id}`)
    setCopiedId(id)
    window.setTimeout(() => setCopiedId(null), 1500)
  }

  const visibleVideos = visibilityFilter ? videos.filter((video) => video.visibility === visibilityFilter) : videos
  const heading = headings[visibilityFilter ?? 'all']

  return (
    <section className="my-videos">
      <div className="section-heading">
        <div>
          <span className="section-kicker">Tu biblioteca</span>
          <h1>{heading.title}</h1>
          <p>{heading.description}</p>
        </div>
        <button className="primary-action" type="button" onClick={onUpload}>Subir video</button>
      </div>
      {error !== null && <ApiFeedback error={error} onRetry={() => void loadVideos()} />}
      {loading ? (
        <p className="panel-status">Cargando tus videos...</p>
      ) : visibleVideos.length === 0 ? (
        <p className="panel-status">Aun no hay elementos disponibles</p>
      ) : (
        <div className="my-video-table">
          {visibleVideos.map((video) => {
            const editing = editingId === video.id
            const uploadingThis = uploading?.id === video.id
            return (
              <article className="my-video-row" key={video.id}>
                <div className="my-video-thumbnail" aria-hidden="true">
                  {video.title.charAt(0).toUpperCase()}
                  <span>{formatDuration(video.durationSeconds)}</span>
                </div>
                <div className="my-video-info">
                  {editing ? (
                    <>
                      <input value={video.title} aria-label="Título" maxLength={200} onChange={(event) => edit(video.id, { title: event.target.value })} />
                      <textarea value={video.description ?? ''} aria-label="Descripción" maxLength={5000} onChange={(event) => edit(video.id, { description: event.target.value })} />
                    </>
                  ) : (
                    <>
                      <h2>{video.title}</h2>
                      <p>{video.description || 'Sin descripción'}</p>
                    </>
                  )}
                  <span className={`status-chip status-chip--${video.status}`}>
                    {uploadingThis
                      ? uploading.progress < 100 ? `Subiendo ${uploading.progress}%` : 'Transcodificando y cifrando...'
                      : statusLabels[video.status]}
                  </span>
                  {video.status === 'ready' && video.visibility !== 'private' && (
                    <button className="my-video-link" type="button" onClick={() => void copyLink(video.id)}>
                      {copiedId === video.id ? 'Enlace copiado' : 'Copiar enlace'}
                    </button>
                  )}
                </div>
                <label className="visibility-field">
                  <span>Visibilidad</span>
                  <select disabled={!editing} value={video.visibility} onChange={(event) => edit(video.id, { visibility: event.target.value as VideoVisibility })}>
                    {(Object.keys(visibilityLabels) as VideoVisibility[]).map((value) => (
                      <option key={value} value={value}>{visibilityLabels[value]}</option>
                    ))}
                  </select>
                </label>
                <div className="my-video-actions">
                  {video.status === 'ready' ? (
                    <button type="button" onClick={() => onPlay(video)}>Reproducir</button>
                  ) : (
                    <label className={`my-video-upload ${uploadingThis ? 'is-busy' : ''}`}>
                      {video.status === 'error' ? 'Reintentar subida' : 'Subir archivo'}
                      <input
                        type="file"
                        accept="video/mp4,video/webm,video/quicktime,video/x-matroska"
                        disabled={uploading !== null}
                        onChange={(event) => {
                          void retryUpload(video.id, event.target.files?.[0])
                          event.target.value = ''
                        }}
                      />
                    </label>
                  )}
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
