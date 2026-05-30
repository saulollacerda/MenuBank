import axios from 'axios'
import { AuthError, type AuthProvider, type AuthSession, type SignUpForm } from './authTypes'

const TOKEN_KEY = 'menubank.auth.token'

const baseURL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.trim() || '/api'
// Bare client (no interceptors) so it never recurses through the app's `api` instance.
const client = axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } })

const listeners: Array<(session: AuthSession | null) => void> = []
function notify(session: AuthSession | null) {
  for (const cb of listeners) cb(session)
}

/** Best-effort read of the `email` claim from the JWT payload (display only). */
function decodeEmail(token: string): string {
  try {
    const payload = token.split('.')[1] ?? ''
    const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    return (json.email as string) ?? ''
  } catch {
    return ''
  }
}

function sessionFromToken(token: string): AuthSession {
  return { accessToken: token, user: { email: decodeEmail(token), user_metadata: {} } }
}

function persist(token: string): AuthSession {
  localStorage.setItem(TOKEN_KEY, token)
  const session = sessionFromToken(token)
  notify(session)
  return session
}

/**
 * Dev auth backend (no Supabase): talks to /api/auth/dev-login and /api/auth/dev-register,
 * which mint a JWT signed by the backend's local RSA key. The token is persisted in
 * localStorage; the merchant already exists after register, so no provision step is needed.
 */
export const localAuthProvider: AuthProvider = {
  async init() {
    const token = localStorage.getItem(TOKEN_KEY)
    return token ? sessionFromToken(token) : null
  },

  onAuthChange(callback) {
    listeners.push(callback)
  },

  async signIn(email, password) {
    try {
      const { data } = await client.post<{ accessToken: string }>('/auth/dev-login', {
        email,
        password,
      })
      return persist(data.accessToken)
    } catch {
      throw new AuthError('invalid_credentials')
    }
  },

  async signUp(form: SignUpForm) {
    try {
      const { data } = await client.post<{ accessToken: string }>('/auth/dev-register', {
        merchantName: form.merchantName,
        cnpj: form.cnpj,
        email: form.email,
        phone: form.phone ?? null,
        password: form.password,
      })
      return { session: persist(data.accessToken) }
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } }).response?.status
      throw new AuthError(status === 409 ? 'email_exists' : 'unknown')
    }
  },

  async signOut() {
    localStorage.removeItem(TOKEN_KEY)
    notify(null)
  },

  async getAccessToken() {
    return localStorage.getItem(TOKEN_KEY)
  },
}
