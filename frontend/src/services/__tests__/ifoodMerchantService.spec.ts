import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ifoodMerchantService, merchantErrorMessage } from '@/services/ifoodMerchantService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('ifoodMerchantService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('getDetails should GET /details and return the store identity', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: { id: 'm-1', name: 'Loja Teste', corporateName: 'Loja Teste LTDA' },
    })

    const result = await ifoodMerchantService.getDetails()

    expect(api.get).toHaveBeenCalledWith('/integrations/ifood/merchant/details')
    expect(result.name).toBe('Loja Teste')
  })

  it('getStatus should GET /status and return the operation list', async () => {
    const status = [
      {
        operation: 'DELIVERY',
        salesChannel: 'IFOOD',
        available: true,
        state: 'OK',
        message: null,
        validations: [],
      },
    ]
    vi.mocked(api.get).mockResolvedValue({ data: status })

    const result = await ifoodMerchantService.getStatus()

    expect(api.get).toHaveBeenCalledWith('/integrations/ifood/merchant/status')
    expect(result).toEqual(status)
  })

  it('getInterruptions should GET /interruptions', async () => {
    vi.mocked(api.get).mockResolvedValue({ data: [] })

    const result = await ifoodMerchantService.getInterruptions()

    expect(api.get).toHaveBeenCalledWith('/integrations/ifood/merchant/interruptions')
    expect(result).toEqual([])
  })

  it('createInterruption should POST /interruptions with the payload', async () => {
    const created = {
      id: 'i-1',
      description: 'Almoço',
      start: '2026-07-20T12:00',
      end: '2026-07-20T13:00',
    }
    vi.mocked(api.post).mockResolvedValue({ data: created })

    const payload = { description: 'Almoço', start: '2026-07-20T12:00', end: '2026-07-20T13:00' }
    const result = await ifoodMerchantService.createInterruption(payload)

    expect(api.post).toHaveBeenCalledWith('/integrations/ifood/merchant/interruptions', payload)
    expect(result).toEqual(created)
  })

  it('deleteInterruption should DELETE /interruptions/{id}', async () => {
    vi.mocked(api.delete).mockResolvedValue({ data: null })

    await ifoodMerchantService.deleteInterruption('i-1')

    expect(api.delete).toHaveBeenCalledWith('/integrations/ifood/merchant/interruptions/i-1')
  })

  it('getOpeningHours should GET /opening-hours', async () => {
    const hours = {
      shifts: [{ id: 's-1', dayOfWeek: 'MONDAY', start: '08:00:00', duration: 600 }],
    }
    vi.mocked(api.get).mockResolvedValue({ data: hours })

    const result = await ifoodMerchantService.getOpeningHours()

    expect(api.get).toHaveBeenCalledWith('/integrations/ifood/merchant/opening-hours')
    expect(result.shifts).toHaveLength(1)
  })

  it('updateOpeningHours should PUT /opening-hours with the shifts', async () => {
    const shifts = [{ dayOfWeek: 'MONDAY' as const, start: '08:00:00', duration: 600 }]
    vi.mocked(api.put).mockResolvedValue({ data: { shifts } })

    const result = await ifoodMerchantService.updateOpeningHours(shifts)

    expect(api.put).toHaveBeenCalledWith('/integrations/ifood/merchant/opening-hours', { shifts })
    expect(result.shifts).toEqual(shifts)
  })
})

describe('merchantErrorMessage', () => {
  it('returns the RFC-7807 ProblemDetail detail message when present', () => {
    const err = { response: { data: { detail: 'Já existe uma pausa nesse período.' } } }

    expect(merchantErrorMessage(err)).toBe('Já existe uma pausa nesse período.')
  })

  it('falls back to a generic pt-BR message when no detail is present', () => {
    expect(merchantErrorMessage(new Error('boom'))).toContain('Tente novamente')
  })

  it('uses the provided fallback when supplied', () => {
    expect(merchantErrorMessage(new Error('boom'), 'Falhou ao salvar.')).toBe('Falhou ao salvar.')
  })
})
