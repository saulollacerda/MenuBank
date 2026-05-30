/// <reference types="vite/client" />

import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
  }
}

interface ImportMetaEnv {
  /** `local` (dev, no Supabase) or `supabase` (default). */
  readonly VITE_AUTH_PROVIDER?: 'local' | 'supabase'
  /** Required only when VITE_AUTH_PROVIDER=supabase. */
  readonly VITE_SUPABASE_URL?: string
  readonly VITE_SUPABASE_ANON_KEY?: string
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
