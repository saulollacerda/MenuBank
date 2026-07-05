import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ifoodAuthService, connectErrorMessage } from '@/services/ifoodAuthService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('ifoodAuthService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('status should GET /integrations/ifood/auth/status and return connected flag', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: { connected: true } })

    const result = await ifoodAuthService.status()

    expect(api.get).toHaveBeenCalledWith('/integrations/ifood/auth/status')
    expect(result).toEqual({ connected: true })
  })

  it('start should POST /integrations/ifood/auth/start and return verificationUrlComplete', async () => {
    const mockData = {
      userCode: 'HJLX-LPSQ',
      verificationUrl: 'https://portal.ifood.com.br/apps/code',
      verificationUrlComplete: 'https://portal.ifood.com.br/apps/code?c=HJLX-LPSQ',
      expiresIn: 600,
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await ifoodAuthService.start()

    expect(api.post).toHaveBeenCalledWith('/integrations/ifood/auth/start')
    expect(result).toEqual(mockData)
    expect(result.verificationUrlComplete).toBe('https://portal.ifood.com.br/apps/code?c=HJLX-LPSQ')
  })

  it('connect should POST /integrations/ifood/auth/connect with authorizationCode', async () => {
    vi.mocked(api.post).mockResolvedValue({ data: undefined })

    await ifoodAuthService.connect('auth-code-123')

    expect(api.post).toHaveBeenCalledWith('/integrations/ifood/auth/connect', {
      authorizationCode: 'auth-code-123',
    })
  })

  it('revoke should DELETE /integrations/ifood/auth/revoke', async () => {
    vi.mocked(api.delete).mockResolvedValue({ data: undefined })

    await ifoodAuthService.revoke()

    expect(api.delete).toHaveBeenCalledWith('/integrations/ifood/auth/revoke')
  })

  describe('connectErrorMessage', () => {
    it('maps 409 to expired-authorization guidance with expired=true', () => {
      const result = connectErrorMessage({ response: { status: 409 } })

      expect(result.expired).toBe(true)
      expect(result.message).toContain('novo código')
    })

    it('maps 400 to invalid-code message with expired=false', () => {
      const result = connectErrorMessage({ response: { status: 400 } })

      expect(result.expired).toBe(false)
      expect(result.message).toContain('inválido')
    })

    it('maps network/unknown errors to a generic message', () => {
      const result = connectErrorMessage(new Error('Network Error'))

      expect(result.expired).toBe(false)
      expect(result.message).toContain('Tente novamente')
    })
  })
})
