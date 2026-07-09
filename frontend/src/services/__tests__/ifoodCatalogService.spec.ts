import { describe, it, expect, vi, beforeEach } from 'vitest'
import { ifoodCatalogService, importErrorMessage } from '@/services/ifoodCatalogService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('ifoodCatalogService', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('importCatalog should POST /integrations/ifood/catalog/import and return the summary', async () => {
    const mockResult = {
      importedProducts: 3,
      linkedProducts: 1,
      skippedProducts: 1,
      importedCategories: 2,
      linkedCategories: 0,
      items: [
        { name: 'X-Burger', externalCode: 'BURGER_001', outcome: 'IMPORTED', reason: null },
        { name: 'Pizza', externalCode: 'PIZZA_01', outcome: 'SKIPPED', reason: 'Item sem preço no catálogo' },
      ],
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockResult })

    const result = await ifoodCatalogService.importCatalog()

    expect(api.post).toHaveBeenCalledWith('/integrations/ifood/catalog/import')
    expect(result.importedProducts).toBe(3)
    expect(result.items).toHaveLength(2)
    expect(result.items[1]?.outcome).toBe('SKIPPED')
  })

  describe('importErrorMessage', () => {
    it('maps 409 to a reconnect/connect-first message', () => {
      const message = importErrorMessage({ response: { status: 409 } })

      expect(message).toContain('Conecte')
    })

    it('maps unknown errors to a generic retry message', () => {
      const message = importErrorMessage(new Error('Network Error'))

      expect(message).toContain('Tente novamente')
    })
  })
})
