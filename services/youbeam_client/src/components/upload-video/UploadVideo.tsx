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
    <section className="upload-panel">
      <div className="section-heading">
        <div>
          <span className="section-kicker">Publicar contenido</span>
          <h1>Subir video</h1>
          <p>El servidor transcodifica el archivo y lo cifra (AES-256-GCM + HPKE) antes de guardarlo.</p>
        </div>
      </div>
      {error !== null && <ApiFeedback error={error} />}
      <form className="upload-form" onSubmit={handleSubmit}>
        <label className="upload-dropzone">
          <strong>{file ? file.name : 'Selecciona un archivo de video'}</strong>
          <span>MP4, WebM, MOV o MKV · máximo 2 GB</span>
          <input type="file" accept="video/mp4,video/webm,video/quicktime,video/x-matroska" required onChange={(event) => setFile(event.target.files?.[0] ?? null)} />
        </label>
        <label>
          <span>Título</span>
          <input value={title} required maxLength={200} onChange={(event) => setTitle(event.target.value)} placeholder="Título del video" />
        </label>
        <label>
          <span>Descripción</span>
          <textarea value={description} maxLength={5000} onChange={(event) => setDescription(event.target.value)} placeholder="Describe tu video" />
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
            <div>
              <span>{progress < 100 ? 'Subiendo archivo' : 'Transcodificando y cifrando en el servidor...'}</span>
              <strong>{progress}%</strong>
            </div>
            {progress < 100 ? <progress value={progress} max="100" /> : <progress />}
            <small>El video queda listo para reproducirse cuando termina el cifrado.</small>
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
