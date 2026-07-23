import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  ifoodCatalogService,
  importErrorMessage,
  publishErrorMessage,
} from '@/services/ifoodCatalogService'
import api from '@/services/api'

vi.mock('@/services/api', () => ({
  default: {
    post: vi.fn(),
    patch: vi.fn(),
    get: vi.fn(),
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

  it('publishCatalog should POST /integrations/ifood/catalog/publish with the selected products', async () => {
    const mockResult = {
      publishedProducts: 2,
      skippedProducts: 1,
      items: [
        {
          productId: 'p1',
          name: 'X-Burger',
          externalCode: 'BURGER_001',
          outcome: 'PUBLISHED',
          reason: null,
        },
        {
          productId: 'p2',
          name: 'Coca-Cola',
          externalCode: 'COKE_001',
          outcome: 'PUBLISHED',
          reason: null,
        },
        {
          productId: 'p3',
          name: 'Pizza',
          externalCode: null,
          outcome: 'SKIPPED',
          reason: 'Produto sem categoria',
        },
      ],
    }
    vi.mocked(api.post).mockResolvedValue({ data: mockResult })

    const result = await ifoodCatalogService.publishCatalog(['p1', 'p2', 'p3'])

    expect(api.post).toHaveBeenCalledWith('/integrations/ifood/catalog/publish', {
      productIds: ['p1', 'p2', 'p3'],
    })
    expect(result.publishedProducts).toBe(2)
    expect(result.items[2]?.outcome).toBe('SKIPPED')
  })

  it('publishCatalog without ids should POST an empty body meaning "all active products"', async () => {
    vi.mocked(api.post).mockResolvedValue({
      data: { publishedProducts: 0, skippedProducts: 0, items: [] },
    })

    await ifoodCatalogService.publishCatalog()

    expect(api.post).toHaveBeenCalledWith('/integrations/ifood/catalog/publish', {})
  })

  it('syncPrices should PATCH /integrations/ifood/catalog/prices and return the batch acceptance', async () => {
    vi.mocked(api.patch).mockResolvedValue({
      data: { batchId: 'batch-1', requested: 2, skipped: [{ productId: 'p3', reason: 'Sem código' }] },
    })

    const result = await ifoodCatalogService.syncPrices(['p1', 'p2'])

    expect(api.patch).toHaveBeenCalledWith('/integrations/ifood/catalog/prices', {
      productIds: ['p1', 'p2'],
    })
    expect(result.batchId).toBe('batch-1')
    expect(result.skipped[0]?.reason).toBe('Sem código')
  })

  it('syncStatus should PATCH /integrations/ifood/catalog/status with each item status', async () => {
    vi.mocked(api.patch).mockResolvedValue({
      data: { batchId: 'batch-2', requested: 1, skipped: [] },
    })

    const result = await ifoodCatalogService.syncStatus([{ productId: 'p1', status: 'UNAVAILABLE' }])

    expect(api.patch).toHaveBeenCalledWith('/integrations/ifood/catalog/status', {
      items: [{ productId: 'p1', status: 'UNAVAILABLE' }],
    })
    expect(result.batchId).toBe('batch-2')
  })

  it('getBatch should GET /integrations/ifood/catalog/batch/{batchId}', async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        batchId: 'batch-1',
        status: 'COMPLETED',
        successCount: 2,
        failureCount: 0,
        results: [{ resourceId: 'p1', result: 'SUCCESS' }],
      },
    })

    const result = await ifoodCatalogService.getBatch('batch-1')

    expect(api.get).toHaveBeenCalledWith('/integrations/ifood/catalog/batch/batch-1')
    expect(result.status).toBe('COMPLETED')
    expect(result.successCount).toBe(2)
  })

  describe('publishErrorMessage', () => {
    it('maps 409 to a connect/reauthorize message', () => {
      expect(publishErrorMessage({ response: { status: 409 } })).toContain('Conecte')
    })

    it('maps 422 to an invalid data message', () => {
      expect(publishErrorMessage({ response: { status: 422 } })).toContain('inválidos')
    })

    it('maps 404 to an item-not-found-on-ifood message', () => {
      expect(publishErrorMessage({ response: { status: 404 } })).toContain('não encontrado')
    })

    it('maps 503 to an iFood unavailable message', () => {
      const message = publishErrorMessage({ response: { status: 503 } })

      expect(message).toContain('indisponível')
      expect(message).toContain('Tente novamente')
    })

    it('maps unknown errors to a generic retry message', () => {
      expect(publishErrorMessage(new Error('Network Error'))).toContain('Tente novamente')
    })
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
