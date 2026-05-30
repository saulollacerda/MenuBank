import { createClient } from '@supabase/supabase-js'

const envUrl = import.meta.env.VITE_SUPABASE_URL
const envAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY

if (!envUrl || !envAnonKey) {
  console.error('VITE_SUPABASE_URL e VITE_SUPABASE_ANON_KEY são obrigatórias (.env)')
}

// Fallbacks keep createClient from throwing when env vars are absent (e.g. unit tests).
const supabaseUrl = envUrl || 'http://localhost:54321'
const supabaseAnonKey = envAnonKey || 'anon-key-placeholder'

/**
 * Supabase client — owns authentication. The session (access + refresh tokens) is
 * persisted in localStorage and refreshed automatically; the API interceptor reads
 * the current access token from here.
 */
export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
  },
})
