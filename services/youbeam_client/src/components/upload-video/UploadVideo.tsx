import { useState } from 'react'
import type { FormEvent } from 'react'
import { uploadVideo } from '../../lib/api'
import type { Video, VideoVisibility } from '../../types/video'
import ApiFeedback from '../api-feedback/ApiFeedback'
import './upload-video.css'

type UploadVideoProps = {
  onCancel: () => void
  onComplete: (video: Video) => void
}

function UploadVideo({ onCancel, onComplete }: UploadVideoProps) {
  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [visibility, setVisibility] = useState<VideoVisibility>('public')
  const [progress, setProgress] = useState(0)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<unknown>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) return
    setUploading(true)
    setProgress(0)
    setError(null)
    try {
      onComplete(await uploadVideo(file, { title: title.trim(), description, visibility }, setProgress))
    } catch (uploadError) {
      setError(uploadError)
    } finally {
      setUploading(false)
    }
  }

  return (
      <section className="uv-pro-layout">
        <div className="uv-pro-surface">
          <header className="uv-pro-header">
            <h1>Nuevo video</h1>
            <p>Sube y configura tu contenido de forma segura</p>
          </header>

          {error !== null && <ApiFeedback error={error} />}

          <form className="uv-pro-form" onSubmit={handleSubmit}>
            <label className={`uv-pro-droparea ${file ? 'has-file' : ''}`}>
              <input
                  type="file"
                  accept="video/mp4,video/webm,video/quicktime,video/x-matroska"
                  required
                  disabled={uploading}
                  onChange={(event) => setFile(event.target.files?.[0] ?? null)}
              />
              <div className="uv-pro-droparea-content">
                {file ? (
                    <>
                      <div className="uv-pro-icon is-success">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="20 6 9 17 4 12"/></svg>
                      </div>
                      <div className="uv-pro-drop-text">
                        <span className="uv-pro-filename">{file.name}</span>
                        <span className="uv-pro-filemeta">{(file.size / (1024 * 1024)).toFixed(2)} MB • Haz clic para cambiar</span>
                      </div>
                    </>
                ) : (
                    <>
                      <div className="uv-pro-icon">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                      </div>
                      <div className="uv-pro-drop-text">
                        <span className="uv-pro-filename">Selecciona o arrastra un video</span>
                        <span className="uv-pro-filemeta">MP4, WebM, MOV o MKV hasta 2 GB</span>
                      </div>
                    </>
                )}
              </div>
            </label>

            <div className="uv-pro-document">
              <div className="uv-pro-field">
                <label>Título</label>
                <input
                    className="uv-pro-input"
                    value={title}
                    required
                    maxLength={200}
                    disabled={uploading}
                    onChange={(event) => setTitle(event.target.value)}
                    placeholder="Ej. Mi primer video increíble"
                />
              </div>

              <div className="uv-pro-field">
                <label>Descripción</label>
                <textarea
                    className="uv-pro-input is-textarea"
                    value={description}
                    maxLength={5000}
                    disabled={uploading}
                    onChange={(event) => setDescription(event.target.value)}
                    placeholder="Cuenta más detalles sobre este video..."
                />
              </div>

              <div className="uv-pro-field">
                <label>Visibilidad</label>
                <div className="uv-pro-select-wrapper">
                  <select
                      className="uv-pro-input is-select"
                      value={visibility}
                      disabled={uploading}
                      onChange={(event) => setVisibility(event.target.value as VideoVisibility)}
                  >
                    <option value="public">Público — Visible en el catálogo</option>
                    <option value="hidden">Oculto — Solo mediante enlace</option>
                    <option value="private">Privado — Solo para ti</option>
                  </select>
                  <div className="uv-pro-select-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><polyline points="6 9 12 15 18 9"/></svg>
                  </div>
                </div>
              </div>
            </div>

            <div className="uv-pro-footer">
              {uploading && (
                  <div className="uv-pro-progress-bar">
                    <div className={`uv-pro-progress-fill ${progress === 100 ? 'is-processing' : ''}`} style={{ width: `${progress}%` }} />
                  </div>
              )}
              <div className="uv-pro-actions">
                {uploading ? (
                    <span className="uv-pro-status-text">
                  {progress < 100 ? `Subiendo ${progress}%` : 'Cifrando contenido...'}
                </span>
                ) : (
                    <span />
                )}
                <div className="uv-pro-btn-group">
                  <button className="uv-pro-btn-text" type="button" disabled={uploading} onClick={onCancel}>
                    Cancelar
                  </button>
                  <button className="uv-pro-btn-primary" type="submit" disabled={!file || uploading}>
                    Publicar
                  </button>
                </div>
              </div>
            </div>
          </form>
        </div>
      </section>
  )
}

export default UploadVideo