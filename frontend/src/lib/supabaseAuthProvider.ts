import type { Session } from '@supabase/supabase-js'
import { supabase } from './supabase'
import { AuthError, type AuthProvider, type AuthSession, type SignUpForm } from './authTypes'

/** Maps a Supabase session to the backend-agnostic {@link AuthSession}. */
function toSession(session: Session | null): AuthSession | null {
  if (!session) return null
  return {
    accessToken: session.access_token,
    user: {
      email: session.user.email ?? '',
      user_metadata: session.user.user_metadata ?? {},
    },
  }
}

// Cached in-memory token updated by onAuthStateChange and init().
// Avoids calling supabase.auth.getSession() on every API request, which would
// trigger concurrent calls and hit Supabase rate limits (HTTP 429).
let cachedToken: string | null = null
// Epoch seconds when the cached token expires (from session.expires_at); null when unknown.
let cachedExpiresAt: number | null = null

// Single-flight guards so concurrent callers share one in-flight network call
// instead of fanning out N getSession()/refreshSession() requests.
let pendingGetSession: Promise<string | null> | null = null
let pendingRefresh: Promise<string | null> | null = null

// Refresh slightly ahead of the real expiry to avoid racing token expiry.
const EXPIRY_SKEW_SECONDS = 30

/** Updates the in-memory token cache from a Supabase session. */
function cacheFromSession(session: Session | null): void {
  cachedToken = session?.access_token ?? null
  cachedExpiresAt = session?.expires_at ?? null
}

/** True when a cached token exists and is not within {@link EXPIRY_SKEW_SECONDS} of expiring. */
function isCachedTokenFresh(): boolean {
  if (!cachedToken) return false
  if (cachedExpiresAt === null) return true // no expiry info — preserve legacy behavior
  return cachedExpiresAt - Math.floor(Date.now() / 1000) > EXPIRY_SKEW_SECONDS
}

/** Removes any persisted Supabase auth token keys (e.g. `sb-<ref>-auth-token`, `-auth-token-user`). */
function clearPersistedAuthTokens(): void {
  try {
    const keys: string[] = []
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key && /^sb-.*-auth-token/.test(key)) keys.push(key)
    }
    for (const key of keys) localStorage.removeItem(key)
  } catch {
    // localStorage may be unavailable (e.g. SSR/tests) — nothing to clear.
  }
}

/** Production auth backend: delegates to Supabase. */
export const supabaseAuthProvider: AuthProvider = {
  async init() {
    const { data } = await supabase.auth.getSession()
    cacheFromSession(data.session)
    return toSession(data.session)
  },

  onAuthChange(callback) {
    supabase.auth.onAuthStateChange((_event, session) => {
      cacheFromSession(session)
      callback(toSession(session))
    })
  },

  async signIn(email, password) {
    const { data, error } = await supabase.auth.signInWithPassword({ email, password })
    if (error) {
      const msg = error.message.toLowerCase()
      const code = msg.includes('email not confirmed')
        ? 'email_not_confirmed'
        : msg.includes('invalid login credentials')
          ? 'invalid_credentials'
          : 'unknown' // network failure, rate limit, misconfiguration — not the user's fault
      throw new AuthError(code, error.message)
    }
    cacheFromSession(data.session)
    return toSession(data.session)!
  },

  async signUp(form: SignUpForm) {
    const { data, error } = await supabase.auth.signUp({
      email: form.email,
      password: form.password,
      options: {
        data: {
          merchantName: form.merchantName,
          cnpj: form.cnpj,
          phone: form.phone ?? null,
        },
        emailRedirectTo: `${window.location.origin}/email-verificado`,
      },
    })
    if (error) {
      const msg = error.message.toLowerCase()
      throw new AuthError(msg.includes('already') ? 'email_exists' : 'unknown', error.message)
    }
    // Email confirmation ON + email already registered: Supabase obfuscates the
    // duplicate as a 200 with an empty identities array and sends no email.
    if (data.user && data.user.identities?.length === 0) {
      throw new AuthError('email_exists')
    }
    // Email confirmation ON → session is null; confirmation OFF → session is set.
    cacheFromSession(data.session)
    return { session: toSession(data.session) }
  },

  async signOut() {
    // Local scope so one tab's sign-out never revokes other devices' refresh tokens.
    const { error } = await supabase.auth
      .signOut({ scope: 'local' })
      .catch((err: unknown) => ({ error: err as Error }))
    if (error) {
      // Sign-out failed remotely (returned error or network/5xx rejection); we still
      // clear all local state below so a failed call can't leave a phantom session.
    }
    cachedToken = null
    cachedExpiresAt = null
    clearPersistedAuthTokens()
  },

  async getAccessToken() {
    if (isCachedTokenFresh()) return cachedToken
    // Cache is missing/stale: getSession() refreshes an expired session. Single-flight
    // so concurrent requests racing token expiry share one call (avoids HTTP 429).
    if (!pendingGetSession) {
      pendingGetSession = supabase.auth
        .getSession()
        .then(({ data }) => {
          cacheFromSession(data.session)
          return cachedToken
        })
        .catch(() => cachedToken)
        .finally(() => {
          pendingGetSession = null
        })
    }
    return pendingGetSession
  },

  async refreshSession() {
    if (!pendingRefresh) {
      pendingRefresh = supabase.auth
        .refreshSession()
        .then(({ data, error }) => {
          if (error) return null
          cacheFromSession(data.session)
          return cachedToken
        })
        .catch(() => null)
        .finally(() => {
          pendingRefresh = null
        })
    }
    return pendingRefresh
  },

  async requestPasswordReset(email) {
    const { error } = await supabase.auth.resetPasswordForEmail(email, {
      redirectTo: `${window.location.origin}/redefinir-senha`,
    })
    if (error) throw new AuthError('unknown', error.message)
  },

  async updatePassword(password) {
    const { error } = await supabase.auth.updateUser({ password })
    if (error) throw new AuthError('unknown', error.message)
  },
}
