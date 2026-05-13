import axios from 'axios'

const configuredBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim()
const baseURL = configuredBaseUrl && configuredBaseUrl.length > 0 ? configuredBaseUrl : '/api'

const api = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('menubank_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const currentPath = window.location.pathname
      if (currentPath !== '/login' && currentPath !== '/register') {
        localStorage.removeItem('menubank_token')
        localStorage.removeItem('menubank_user')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  },
)

export default api
