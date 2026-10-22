export type VideoVisibility = 'public' | 'hidden' | 'private'

export type Video = {
  id: string
  title: string
  description?: string
  visibility: VideoVisibility
  status?: 'processing' | 'ready' | 'failed'
  creator: string
  views: string
  publishedAt: string
  duration: string
  thumbnail: string
  avatar?: string
  badge?: string
  verified?: boolean
}
