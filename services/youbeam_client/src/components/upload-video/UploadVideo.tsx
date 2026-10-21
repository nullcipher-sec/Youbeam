import { useState } from 'react'
import type { FormEvent } from 'react'
import { uploadVideo } from '../../lib/api'
import type { VideoVisibility } from '../../types/video'
import ApiFeedback from '../api-feedback/ApiFeedback'
import './upload-video.css'

type UploadVideoProps = {
  onCancel: () => void
  onComplete: () => void
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
    setError(null)
    try {
      await uploadVideo(file, { title, description, visibility }, setProgress)
      onComplete()
    } catch (uploadError) {
      setError(uploadError)
    } finally {
      setUploading(false)
    }
  }

  return (
    <section className="upload-panel">
      <div className="section-heading">
        <div>
          <span className="section-kicker">Publicar contenido</span>
          <h1>Subir video</h1>
          <p>El archivo se procesará en segundo plano hasta quedar listo para reproducirse.</p>
        </div>
      </div>
      {error !== null && <ApiFeedback error={error} />}
      <form className="upload-form" onSubmit={handleSubmit}>
        <label className="upload-dropzone">
          <strong>{file ? file.name : 'Selecciona un archivo de video'}</strong>
          <span>MP4, WebM o MOV · el backend validará tamaño y códec</span>
          <input type="file" accept="video/*" required onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        </label>
        <label>
          <span>Título</span>
          <input value={title} required maxLength={120} onChange={(event) => setTitle(event.target.value)} placeholder="Título del video" />
        </label>
        <label>
          <span>Descripción</span>
          <textarea value={description} maxLength={2000} onChange={(event) => setDescription(event.target.value)} placeholder="Describe tu video" />
        </label>
        <label>
          <span>Visibilidad</span>
          <select value={visibility} onChange={(event) => setVisibility(event.target.value as VideoVisibility)}>
            <option value="public">Público — aparece en el catálogo</option>
            <option value="hidden">Oculto — accesible mediante enlace</option>
            <option value="private">Privado — solo para ti</option>
          </select>
        </label>
        {uploading && (
          <div className="upload-progress" aria-live="polite">
            <div><span>Subiendo y procesando</span><strong>{progress}%</strong></div>
            <progress value={progress} max="100" />
            <small>El estado cambiará de processing a ready cuando el backend termine.</small>
          </div>
        )}
        <div className="upload-actions">
          <button className="secondary-action" type="button" disabled={uploading} onClick={onCancel}>Cancelar</button>
          <button className="upload-submit" type="submit" disabled={!file || uploading}>
            {uploading ? 'Subiendo...' : 'Publicar video'}
          </button>
        </div>
      </form>
    </section>
  )
}

export default UploadVideo
