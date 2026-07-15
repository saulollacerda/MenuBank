import { describe, it, expect, vi, beforeEach } from 'vitest'
import type { InternalAxiosRequestConfig } from 'axios'

const getAccessToken = vi.fn()
const refreshSession = vi.fn()
const signOut = vi.fn()
const routerPush = vi.fn()

vi.mock('@/lib/authProvider', () => ({
  authProvider: {
    getAccessToken: (...a: unknown[]) => getAccessToken(...a),
    refreshSession: (...a: unknown[]) => refreshSession(...a),
    signOut: (...a: unknown[]) => signOut(...a),
  },
}))

vi.mock('@/router', () => ({
  default: { push: (...a: unknown[]) => routerPush(...a) },
}))

import api from '@/services/api'

type RequestHandler = {
  fulfilled: (c: InternalAxiosRequestConfig) => Promise<InternalAxiosRequestConfig>
}

type ResponseHandler = {
  fulfilled: (r: unknown) => unknown
  rejected: (e: unknown) => Promise<unknown>
}

function requestInterceptor(): RequestHandler {
  // axios stores registered interceptors in a private handlers array
  const handlers = (api.interceptors.request as unknown as { handlers: RequestHandler[] }).handlers
  return handlers[0]!
}

function responseInterceptor(): ResponseHandler {
  const handlers = (api.interceptors.response as unknown as { handlers: ResponseHandler[] }).handlers
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

describe('api response interceptor (refresh on 401)', () => {
  it('401 → refresh bem-sucedido → repete a requisição e devolve a resposta, sem signOut/redirect', async () => {
    refreshSession.mockResolvedValue('newtok')
    const requestSpy = vi.spyOn(api, 'request').mockResolvedValue({ data: 'ok', status: 200 })
    const error = { response: { status: 401 }, config: { headers: {}, url: '/orders' } }

    const result = await responseInterceptor().rejected(error)

    expect(refreshSession).toHaveBeenCalledTimes(1)
    expect(requestSpy).toHaveBeenCalledTimes(1)
    expect((requestSpy.mock.calls[0]![0] as { _retried?: boolean })._retried).toBe(true)
    expect(result).toMatchObject({ data: 'ok' })
    expect(signOut).not.toHaveBeenCalled()
    expect(routerPush).not.toHaveBeenCalled()
    requestSpy.mockRestore()
  })

  it('401 → refresh retorna null → faz signOut e redireciona para login', async () => {
    refreshSession.mockResolvedValue(null)
    const error = { response: { status: 401 }, config: { headers: {}, url: '/orders' } }

    await expect(responseInterceptor().rejected(error)).rejects.toBe(error)

    expect(refreshSession).toHaveBeenCalledTimes(1)
    expect(signOut).toHaveBeenCalledTimes(1)
    expect(routerPush).toHaveBeenCalledWith({ name: 'login' })
  })

  it('requisição já repetida que retorna 401 de novo não gera loop: signOut uma vez', async () => {
    const error = {
      response: { status: 401 },
      config: { headers: {}, url: '/orders', _retried: true },
    }

    await expect(responseInterceptor().rejected(error)).rejects.toBe(error)

    expect(refreshSession).not.toHaveBeenCalled()
    expect(signOut).toHaveBeenCalledTimes(1)
    expect(routerPush).toHaveBeenCalledWith({ name: 'login' })
  })

  it('erros não-401 passam sem alteração', async () => {
    const error = { response: { status: 500 }, config: { headers: {}, url: '/orders' } }

    await expect(responseInterceptor().rejected(error)).rejects.toBe(error)

    expect(refreshSession).not.toHaveBeenCalled()
    expect(signOut).not.toHaveBeenCalled()
    expect(routerPush).not.toHaveBeenCalled()
  })
})
