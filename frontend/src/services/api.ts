import axios, { type InternalAxiosRequestConfig } from 'axios'
import { authProvider } from '@/lib/authProvider'

const configuredBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim()
const baseURL = configuredBaseUrl && configuredBaseUrl.length > 0 ? configuredBaseUrl : '/api'

const api = axios.create({
  baseURL,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use(async (config) => {
  const token = await authProvider.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Prevents concurrent 401 responses (e.g. from polling) from triggering
// multiple signOut + redirect calls simultaneously.
let redirectingToLogin = false

// Marks a request that already had its token refreshed and was retried once,
// so a second 401 falls through to sign-out instead of looping forever.
type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config as RetriableConfig | undefined
    const isAuthPath =
      window.location.pathname === '/login' || window.location.pathname === '/register'

    // A stale/expired token yields a 401. Before signing out, try one token refresh
    // and replay the request — refresh returning null (no valid session) is terminal.
    if (error.response?.status === 401 && originalRequest && !originalRequest._retried && !isAuthPath) {
      const token = await authProvider.refreshSession()
      if (token) {
        originalRequest._retried = true
        // Re-run through the same instance; the request interceptor attaches the fresh token.
        return api.request(originalRequest)
      }
    }

    if (error.response?.status === 401 && !redirectingToLogin && !isAuthPath) {
      redirectingToLogin = true
      try {
        await authProvider.signOut()
      } catch {
        // signOut errors must not block the redirect
      }
      const { default: router } = await import('@/router')
      await router.push({ name: 'login' })
      redirectingToLogin = false
    }
    return Promise.reject(error)
  },
)

export default api
