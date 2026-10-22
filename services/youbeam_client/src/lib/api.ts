import type { Video, VideoDraft } from '../types/video'

const API_PREFIX = '/api'

// Usuario autenticado (UserResponse del Core)
export type CurrentUser = {
  id: string
  username: string
  email: string
  displayName: string | null
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

// El Core responde los errores como { status, code, message }; si no, se usa el texto tal cual
function errorMessage(body: string, status: number): string {
  try {
    const parsed: unknown = JSON.parse(body)
    if (typeof parsed === 'object' && parsed !== null && 'message' in parsed) return String(parsed.message)
  } catch {
    // El cuerpo no es JSON
  }
  return body || `La petición falló con estado ${status}.`
}

export async function apiFetch<T>(
  path: string,
  init: Omit<RequestInit, 'credentials'> = {},
): Promise<T> {
  // La cookie de sesión es HttpOnly: el navegador la envía sola, el código nunca la ve
  const response = await fetch(apiUrl(path), {
    ...init,
    credentials: 'include',
    headers: { Accept: 'application/json', ...init.headers },
  })

  if (!response.ok) {
    throw new ApiError(errorMessage(await response.text(), response.status), response.status)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}

function jsonBody(method: string, body: unknown): Omit<RequestInit, 'credentials'> {
  return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) }
}

export function getApiErrorMessage(error: unknown): string {
  // Errores del navegador (p. ej. WebRTC) traen un mensaje propio, que es más útil que uno genérico
  if (!(error instanceof ApiError)) {
    return error instanceof Error && error.message ? error.message : 'Ocurrió un error inesperado. Inténtalo de nuevo.'
  }
  switch (error.status) {
    case 0: return error.message
    case 400: return 'Los datos enviados no son válidos. Revisa el formulario.'
    case 401: return 'Tu sesión ha expirado o no has iniciado sesión.'
    case 403: return 'No tienes permisos para realizar esta acción.'
    case 404: return 'El recurso solicitado no existe o ya no está disponible.'
    case 409: return error.message === 'Username or email already registered'
      ? 'El usuario o el correo ya están registrados.'
      : 'La operación entra en conflicto con el estado actual del video.'
    case 413: return 'El archivo supera el tamaño máximo permitido.'
    case 415: return 'Formato no admitido. Usa MP4, WebM, MOV o MKV.'
    case 422: return 'El archivo no es un video válido o no se pudo procesar.'
    case 502: return 'El servicio de streaming no está disponible.'
    default: return error.message
  }
}

// ── Autenticación ──

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

export function register(data: { username: string; email: string; password: string; displayName?: string }) {
  return apiFetch<CurrentUser>('/auth/register', jsonBody('POST', data))
}

export function login(data: { login: string; password: string }) {
  return apiFetch<CurrentUser>('/auth/login', jsonBody('POST', data))
}

export function logout() {
  return apiFetch<void>('/auth/logout', { method: 'POST' })
}

// ── Videos ──

export function listCatalog(page: number, size: number): Promise<Video[]> {
  return apiFetch<Video[]>(`/videos/catalog?page=${page}&size=${size}`)
}

export function listMyVideos(): Promise<Video[]> {
  return apiFetch<Video[]>('/videos')
}

export function getVideo(id: string): Promise<Video> {
  return apiFetch<Video>(`/videos/${encodeURIComponent(id)}`)
}

export function createVideo(draft: VideoDraft): Promise<Video> {
  return apiFetch<Video>('/videos', jsonBody('POST', draft))
}

export function updateVideo(id: string, changes: Partial<VideoDraft>): Promise<Video> {
  return apiFetch<Video>(`/videos/${encodeURIComponent(id)}`, jsonBody('PATCH', changes))
}

export async function deleteVideo(id: string): Promise<void> {
  await apiFetch<void>(`/videos/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

/**
 * Sube el archivo de un video ya creado. Se usa XMLHttpRequest porque fetch no informa del
 * progreso de subida. Al llegar al 100 % el Core todavía transcodifica y cifra el archivo,
 * así que la respuesta puede tardar.
 */
export function uploadVideoFile(id: string, file: File, onProgress: (progress: number) => void): Promise<Video> {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest()
    const formData = new FormData()
    formData.append('file', file)
    request.open('POST', apiUrl(`/videos/${encodeURIComponent(id)}/upload`))
    request.withCredentials = true
    request.setRequestHeader('Accept', 'application/json')
    request.upload.addEventListener('progress', (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100))
    })
    request.addEventListener('error', () => reject(new ApiError('No se pudo conectar con el servidor.', 0)))
    request.addEventListener('abort', () => reject(new ApiError('La subida fue cancelada.', 0)))
    request.addEventListener('load', () => {
      if (request.status < 200 || request.status >= 300) {
        reject(new ApiError(errorMessage(request.responseText, request.status), request.status))
        return
      }
      try {
        resolve(JSON.parse(request.responseText) as Video)
      } catch {
        reject(new ApiError('El servidor devolvió una respuesta inválida.', request.status))
      }
    })
    request.send(formData)
  })
}

/**
 * Publicar un video son dos pasos en el Core: crear sus metadatos y subir el archivo.
 * Si la subida falla, el video queda creado y se puede reintentar desde "Mis videos".
 */
export async function uploadVideo(file: File, draft: VideoDraft, onProgress: (progress: number) => void): Promise<Video> {
  const video = await createVideo(draft)
  return uploadVideoFile(video.id, file, onProgress)
}

// ── Reproducción ──

export async function requestPlaybackTicket(videoId: string): Promise<string> {
  const response = await apiFetch<{ ticket?: string }>(`/videos/${encodeURIComponent(videoId)}/play`)
  if (!response.ticket) throw new ApiError('La respuesta no incluye el ticket de reproducción.', 502)
  return response.ticket
}

export async function negotiateStreamOffer(sdp: string, ticket: string): Promise<string> {
  const response = await apiFetch<{ sdp?: string }>('/stream/offer', jsonBody('POST', { sdp, ticket }))
  if (!response.sdp) throw new ApiError('La respuesta no incluye la respuesta WebRTC.', 502)
  return response.sdp
}
