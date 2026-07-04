import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ifoodAuthService } from '@/services/ifoodAuthService'
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

  it('start should POST /integrations/ifood/auth/start', async () => {
    const mockData = {
      userCode: 'HJLX-LPSQ',
      verificationUrl: 'https://portal.ifood.com.br/apps/code',
      expiresIn: 600,
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockData })

    const result = await ifoodAuthService.start()

    expect(api.post).toHaveBeenCalledWith('/integrations/ifood/auth/start')
    expect(result).toEqual(mockData)
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
})
