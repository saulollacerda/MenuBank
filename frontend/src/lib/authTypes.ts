/** Minimal user shape the store needs, regardless of the auth backend. */
export interface AuthUser {
  email: string
  user_metadata: Record<string, unknown>
}

/** Backend-agnostic session: an access token plus the authenticated user. */
export interface AuthSession {
  accessToken: string
  user: AuthUser
}

/** Business data + credentials for registration. */
export interface SignUpForm {
  email: string
  password: string
  merchantName: string
  cnpj: string
  phone?: string | null
}

/** signUp returns no session when the backend requires email confirmation (Supabase). */
export interface SignUpResult {
  session: AuthSession | null
}

export type AuthErrorCode =
  | 'invalid_credentials'
  | 'email_not_confirmed'
  | 'email_exists'
  | 'unknown'

/** Normalized auth error so the store maps to pt-BR messages without knowing the backend. */
export class AuthError extends Error {
  constructor(
    public readonly code: AuthErrorCode,
    message?: string,
  ) {
    super(message ?? code)
    this.name = 'AuthError'
  }
}

/**
 * Authentication backend abstraction. Two implementations exist: Supabase (prod) and a
 * local backend (dev, no Supabase). Selected by `VITE_AUTH_PROVIDER`.
 */
export interface AuthProvider {
  /** Restores a persisted session on app start (null if none). */
  init(): Promise<AuthSession | null>
  /** Subscribes to session changes (refresh, sign-out). */
  onAuthChange(callback: (session: AuthSession | null) => void): void
  /** Signs in; throws {@link AuthError} on failure. */
  signIn(email: string, password: string): Promise<AuthSession>
  /** Registers; throws {@link AuthError} on failure. */
  signUp(form: SignUpForm): Promise<SignUpResult>
  signOut(): Promise<void>
  /** Current access token for the Authorization header (null if signed out). */
  getAccessToken(): Promise<string | null>
}
