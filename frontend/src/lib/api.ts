import axios, { AxiosError } from 'axios'

const TOKEN_KEY = 'hisabkitab.token'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
})

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string | null) {
  if (token) localStorage.setItem(TOKEN_KEY, token)
  else localStorage.removeItem(TOKEN_KEY)
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

/** A 401 means the token expired or was revoked — drop it and bounce to login. */
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401 && !error.config?.url?.includes('/auth/login')) {
      setToken(null)
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

interface ApiErrorBody {
  message?: string
  fieldErrors?: Record<string, string>
}

/** Pulls the human-readable message the backend sends, with a sane fallback. */
export function errorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiErrorBody | undefined
    if (body?.fieldErrors) {
      const first = Object.values(body.fieldErrors)[0]
      if (first) return first
    }
    if (body?.message) return body.message
    if (error.code === 'ERR_NETWORK') return 'Cannot reach the server. Is the backend running?'
  }
  return fallback
}
