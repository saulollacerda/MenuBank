import type { AuthProvider } from './authTypes'
import { supabaseAuthProvider } from './supabaseAuthProvider'
import { localAuthProvider } from './localAuthProvider'

export * from './authTypes'

const providerName = (import.meta.env.VITE_AUTH_PROVIDER as string | undefined)?.trim() || 'supabase'

/** Active auth backend. `local` for dev without Supabase; `supabase` otherwise (default). */
export const authProvider: AuthProvider =
  providerName === 'local' ? localAuthProvider : supabaseAuthProvider
