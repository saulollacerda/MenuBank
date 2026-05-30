import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { InternalAxiosRequestConfig } from 'axios'

const getAccessToken = vi.fn()

vi.mock('@/lib/authProvider', () => ({
  authProvider: {
    getAccessToken: (...a: unknown[]) => getAccessToken(...a),
    signOut: vi.fn(),
  },
}))

import api from '@/services/api'

type Handler = {
  fulfilled: (c: InternalAxiosRequestConfig) => Promise<InternalAxiosRequestConfig>
}

function requestInterceptor(): Handler {
  // axios stores registered interceptors in a private handlers array
  const handlers = (api.interceptors.request as unknown as { handlers: Handler[] }).handlers
  return handlers[0]!
}

beforeEach(() => {
  vi.clearAllMocks()
})

describe('api request interceptor', () => {
  it('anexa o access token como Bearer', async () => {
    getAccessToken.mockResolvedValue('abc123')

    const config = { headers: {} } as unknown as InternalAxiosRequestConfig
    const result = await requestInterceptor().fulfilled(config)

    expect(result.headers.Authorization).toBe('Bearer abc123')
  })

  it('não anexa header quando não há token', async () => {
    getAccessToken.mockResolvedValue(null)

    const config = { headers: {} } as unknown as InternalAxiosRequestConfig
    const result = await requestInterceptor().fulfilled(config)

    expect(result.headers.Authorization).toBeUndefined()
  })
})
