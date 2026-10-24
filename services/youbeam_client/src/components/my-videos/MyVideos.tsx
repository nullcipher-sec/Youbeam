import { useCallback, useEffect, useState } from 'react'
import { deleteVideo, listMyVideos, updateVideo, uploadVideoFile } from '../../lib/api'
import { formatDuration, statusLabels, visibilityLabels } from '../../lib/format'
import type { Video, VideoVisibility } from '../../types/video'
import ApiFeedback from '../api-feedback/ApiFeedback'
import './my-videos.css'

type MyVideosProps = {
  onUpload: () => void
  onPlay: (video: Video) => void
  visibilityFilter?: VideoVisibility
}

const headings: Record<VideoVisibility | 'all', { title: string; description: string }> = {
  all: { title: 'Tus videos', description: 'Administra tu contenido, ajusta detalles y controla quién puede verlo.' },
  private: { title: 'Privados', description: 'Contenido exclusivo. Solo tú puedes reproducirlo.' },
  hidden: { title: 'Ocultos', description: 'Fuera del catálogo. Solo accesibles mediante enlace directo.' },
  public: { title: 'Públicos', description: 'Visibles para cualquier persona en el catálogo principal.' },
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
    if (!window.confirm('¿Eliminar este video de forma permanente?')) return
    try {
      await deleteVideo(id)
      setVideos((current) => current.filter((video) => video.id !== id))
    } catch (deleteError) {
      setError(deleteError)
    }
  }

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

  async function copyLink(id: string) {
    await navigator.clipboard.writeText(`${window.location.origin}/watch/${id}`)
    setCopiedId(id)
    window.setTimeout(() => setCopiedId(null), 2000)
  }

  const visibleVideos = visibilityFilter ? videos.filter((video) => video.visibility === visibilityFilter) : videos
  const heading = headings[visibilityFilter ?? 'all']

  return (
      <section className="mv-container">
        <div className="mv-header-card">
          <div className="mv-header-content">
            <h1>{heading.title}</h1>
            <p>{heading.description}</p>
          </div>
          <button className="mv-btn-primary" type="button" onClick={onUpload}>
            <span aria-hidden="true">+</span> Subir video
          </button>
        </div>

        {error !== null && <ApiFeedback error={error} onRetry={() => void loadVideos()} />}

        {loading ? (
            <div className="mv-empty-state">
              <div className="mv-spinner"></div>
              <p>Cargando tu biblioteca...</p>
            </div>
        ) : visibleVideos.length === 0 ? (
            <div className="mv-empty-state">
              <div className="mv-empty-icon">📁</div>
              <p>No hay videos en esta sección.</p>
            </div>
        ) : (
            <div className="mv-card-list">
              {visibleVideos.map((video) => {
                const isEditing = editingId === video.id
                const isUploading = uploading?.id === video.id
                const uploadProgress = uploading?.progress ?? 0

                return (
                    <article className={`mv-card ${isEditing ? 'is-editing' : ''}`} key={video.id}>
                      <div
                          className="mv-card-media"
                          onClick={() => !isEditing && video.status === 'ready' && onPlay(video)}
                          role={video.status === 'ready' && !isEditing ? "button" : undefined}
                      >
                        <div className="mv-thumb-gradient" aria-hidden="true">
                          {video.title.charAt(0).toUpperCase()}
                        </div>
                        <div className="mv-media-badges">
                          <span className="mv-badge-duration">{formatDuration(video.durationSeconds)}</span>
                        </div>
                        {video.status === 'ready' && !isEditing && (
                            <div className="mv-play-btn-overlay">
                              <svg viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
                            </div>
                        )}
                      </div>

                      <div className="mv-card-body">
                        {isEditing ? (
                            <div className="mv-edit-form">
                              <div className="mv-input-group">
                                <label>Título</label>
                                <input className="mv-input" value={video.title} onChange={(e) => edit(video.id, { title: e.target.value })} autoFocus />
                              </div>
                              <div className="mv-input-group">
                                <label>Descripción</label>
                                <textarea className="mv-textarea" value={video.description ?? ''} onChange={(e) => edit(video.id, { description: e.target.value })} />
                              </div>
                              <div className="mv-input-group">
                                <label>Visibilidad</label>
                                <select className="mv-select" value={video.visibility} onChange={(e) => edit(video.id, { visibility: e.target.value as VideoVisibility })}>
                                  {(Object.keys(visibilityLabels) as VideoVisibility[]).map((v) => (
                                      <option key={v} value={v}>{visibilityLabels[v]}</option>
                                  ))}
                                </select>
                              </div>
                              <div className="mv-edit-actions">
                                <button className="mv-btn-solid" disabled={savingId === video.id} onClick={() => void saveVideo(video)}>
                                  {savingId === video.id ? 'Guardando...' : 'Guardar cambios'}
                                </button>
                                <button className="mv-btn-outline" onClick={() => setEditingId(null)}>Cancelar</button>
                              </div>
                            </div>
                        ) : (
                            <>
                              <div className="mv-card-info">
                                <div className="mv-card-title-row">
                                  <h2 className="mv-title">{video.title}</h2>
                                  <div className="mv-status-badges">
                            <span className={`mv-badge mv-badge-${video.visibility}`}>
                              {visibilityLabels[video.visibility]}
                            </span>
                                    <span className={`mv-badge mv-badge-status-${video.status}`}>
                              {isUploading ? `${uploadProgress}%` : statusLabels[video.status]}
                            </span>
                                  </div>
                                </div>
                                <p className="mv-desc">{video.description || 'Sin descripción añadida.'}</p>
                              </div>

                              <div className="mv-card-footer">
                                <div className="mv-actions-left">
                                  {video.status === 'ready' && video.visibility !== 'private' && (
                                      <button className="mv-btn-tonal" onClick={() => void copyLink(video.id)}>
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" /><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" /></svg>
                                        {copiedId === video.id ? 'Copiado' : 'Copiar enlace'}
                                      </button>
                                  )}

                                  {video.status !== 'ready' && (
                                      <label className={`mv-btn-tonal mv-upload-btn ${isUploading ? 'is-disabled' : ''}`}>
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                                        {video.status === 'error' ? 'Reintentar' : 'Subir archivo'}
                                        <input type="file" accept="video/mp4,video/webm,video/quicktime,video/x-matroska" disabled={isUploading} onChange={(e) => {
                                          void retryUpload(video.id, e.target.files?.[0]); e.target.value = ''
                                        }}/>
                                      </label>
                                  )}
                                </div>

                                <div className="mv-actions-right">
                                  <button className="mv-btn-icon" onClick={() => setEditingId(video.id)} aria-label="Editar">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                                  </button>
                                  <button className="mv-btn-icon is-danger" onClick={() => void removeVideo(video.id)} aria-label="Borrar">
                                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                                  </button>
                                </div>
                              </div>
                            </>
                        )}
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