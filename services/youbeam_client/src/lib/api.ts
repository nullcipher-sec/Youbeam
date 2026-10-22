import type { Video, VideoVisibility } from '../types/video'

const API_PREFIX = '/api'

export type CurrentUser = {
  id: string
  username?: string
  displayName?: string
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }

}

function apiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return normalizedPath === API_PREFIX || normalizedPath.startsWith(`${API_PREFIX}/`)
    ? normalizedPath
    : `${API_PREFIX}${normalizedPath}`
}

export async function apiFetch<T>(
  path: string,
  init: Omit<RequestInit, 'credentials'> = {},
): Promise<T> {
  const response = await fetch(apiUrl(path), {
    ...init,
    credentials: 'include',
    headers: { Accept: 'application/json', ...init.headers },
  })

  if (!response.ok) {
    const message = await response.text()
    throw new ApiError(message || `API request failed with status ${response.status}`, response.status)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

export function getApiErrorMessage(error: unknown): string {
  if (!(error instanceof ApiError)) return 'Ocurrió un error inesperado. Inténtalo de nuevo.'
  if (error.status === 401) return 'Tu sesión ha expirado. Inicia sesión para continuar.'
  if (error.status === 403) return 'No tienes permisos para realizar esta acción.'
  if (error.status === 404) return 'El recurso solicitado no existe o ya no está disponible.'
  return error.message
}

export async function getCurrentUser(): Promise<CurrentUser | null> {
  try {
    return await apiFetch<CurrentUser>('/auth/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      return null
    }
    throw error
  }
}

export async function requestPlaybackTicket(videoId: string): Promise<string> {
  const response = await apiFetch<{ ticket?: string } | string>(`/videos/${encodeURIComponent(videoId)}/play`)
  const ticket = typeof response === 'string' ? response : response.ticket
  if (!ticket) throw new ApiError('The playback ticket was not included in the API response.', 502)
  return ticket
}

export async function negotiateStreamOffer(sdp: string, ticket: string): Promise<string> {
  const response = await apiFetch<{ sdp?: string } | string>('/stream/offer', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sdp, ticket }),
  })
  const answerSdp = typeof response === 'string' ? response : response.sdp
  if (!answerSdp) throw new ApiError('The WebRTC answer was not included in the API response.', 502)
  return answerSdp
}

export async function listPublicVideos(): Promise<Video[]> {
  return apiFetch<Video[]>('/videos/public')
}

export async function listMyVideos(): Promise<Video[]> {
  return apiFetch<Video[]>('/videos/me')
}

export async function updateVideo(
  id: string,
  changes: Pick<Video, 'title' | 'description' | 'visibility'>,
): Promise<Video> {
  return apiFetch<Video>(`/videos/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(changes),
  })
}

export async function deleteVideo(id: string): Promise<void> {
  await apiFetch<void>(`/videos/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

export function uploadVideo(
  file: File,
  metadata: { title: string; description: string; visibility: VideoVisibility },
  onProgress: (progress: number) => void,
): Promise<Video> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest()
    const formData = new FormData()
    formData.append('file', file)
    formData.append('title', metadata.title)
    formData.append('description', metadata.description)
    formData.append('visibility', metadata.visibility)
    request.open('POST', `${API_PREFIX}/videos`)
    request.withCredentials = true
    request.setRequestHeader('Accept', 'application/json')
    request.upload.addEventListener('progress', (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100))
    })
    request.addEventListener('error', () => reject(new ApiError('No se pudo conectar con el servidor.', 0)))
    request.addEventListener('abort', () => reject(new ApiError('La subida fue cancelada.', 0)))
    request.addEventListener('load', () => {
      let response: unknown
      try {
        response = request.responseText ? JSON.parse(request.responseText) : undefined
      } catch {
        reject(new ApiError('El servidor devolvió una respuesta inválida.', request.status))
        return
      }
      if (request.status < 200 || request.status >= 300) {
        reject(new ApiError(
          typeof response === 'object' && response !== null && 'message' in response
            ? String(response.message)
            : `La subida falló con estado ${request.status}.`,
          request.status,
        ))
        return
      }
      resolve(response as Video)
    })
    request.send(formData)
  })
}
