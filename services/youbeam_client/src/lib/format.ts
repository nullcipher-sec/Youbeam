import type { VideoStatus, VideoVisibility } from '../types/video'

// Convierte segundos a mm:ss o h:mm:ss para la etiqueta de duración
export function formatDuration(seconds: number | null): string {
  if (seconds === null || seconds < 0) return '--:--'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const rest = String(seconds % 60).padStart(2, '0')
  return hours > 0 ? `${hours}:${String(minutes).padStart(2, '0')}:${rest}` : `${minutes}:${rest}`
}

const relativeTime = new Intl.RelativeTimeFormat('es', { numeric: 'auto' })

const units: Array<[Intl.RelativeTimeFormatUnit, number]> = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
]

// Fecha relativa en español ("hace 2 días"); el Core envía fechas ISO sin zona horaria
export function formatRelativeDate(isoDate: string | null): string {
  if (!isoDate) return 'sin publicar'
  const elapsed = (Date.now() - new Date(isoDate).getTime()) / 1000
  for (const [unit, size] of units) {
    if (Math.abs(elapsed) >= size) return relativeTime.format(-Math.round(elapsed / size), unit)
  }
  return 'hace un momento'
}

export const visibilityLabels: Record<VideoVisibility, string> = {
  public: 'Público',
  hidden: 'Oculto',
  private: 'Privado',
}

export const statusLabels: Record<VideoStatus, string> = {
  processing: 'Procesando',
  ready: 'Listo',
  error: 'Error',
}
