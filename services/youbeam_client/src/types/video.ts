export type VideoVisibility = 'public' | 'hidden' | 'private'

export type VideoStatus = 'processing' | 'ready' | 'error'

// Metadatos de un video tal como los devuelve el Core (VideoResponse)
export type Video = {
  id: string
  ownerId: string
  title: string
  description: string | null
  visibility: VideoVisibility
  status: VideoStatus
  durationSeconds: number | null
  publishedAt: string | null
  createdAt: string
  updatedAt: string
}

// Datos que el usuario escribe al crear o editar un video
export type VideoDraft = {
  title: string
  description: string
  visibility: VideoVisibility
}
