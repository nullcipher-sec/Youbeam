const API_PREFIX = '/api'

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

  if (normalizedPath === API_PREFIX || normalizedPath.startsWith(`${API_PREFIX}/`)) {
    return normalizedPath
  }

  return `${API_PREFIX}${normalizedPath}`
}

export async function apiFetch<T>(
  path: string,
  init: Omit<RequestInit, 'credentials'> = {},
): Promise<T> {
  const response = await fetch(apiUrl(path), {
    ...init,
    credentials: 'include',
    headers: {
      Accept: 'application/json',
      ...init.headers,
    },
  })

  if (!response.ok) {
    const message = await response.text()
    throw new ApiError(
      message || `API request failed with status ${response.status}`,
      response.status,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await response.json()) as T
}
