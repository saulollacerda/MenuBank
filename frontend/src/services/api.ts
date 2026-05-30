import axios from 'axios'
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

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401 && !redirectingToLogin) {
      const currentPath = window.location.pathname
      if (currentPath !== '/login' && currentPath !== '/register') {
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
    }
    return Promise.reject(error)
  },
)

export default api
