import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, enableAutoUnmount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import IfoodCatalogPublishModal from '@/components/IfoodCatalogPublishModal.vue'
import {
  ifoodCatalogService,
  type IfoodCatalogBatchAccepted,
  type IfoodCatalogBatchStatus,
  type IfoodCatalogPublishResult,
} from '@/services/ifoodCatalogService'
import { useProductStore } from '@/stores/productStore'
import type { ProductResponse } from '@/types/Product'

vi.mock('@/services/ifoodCatalogService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/ifoodCatalogService')>()
  return {
    ...original,
    ifoodCatalogService: {
      publishCatalog: vi.fn(),
      syncPrices: vi.fn(),
      syncStatus: vi.fn(),
      getBatch: vi.fn(),
    },
  }
})

const mockedService = vi.mocked(ifoodCatalogService)

const PRODUCTS: ProductResponse[] = [
  {
    id: 'p1',
    name: 'X-Burger',
    price: 25,
    status: 'ACTIVE',
    categoryId: 'c1',
    categoryName: 'Lanches',
  },
  {
    id: 'p2',
    name: 'Coca-Cola Lata',
    price: 8,
    status: 'ACTIVE',
    categoryId: 'c2',
    categoryName: 'Bebidas',
  },
  {
    id: 'p3',
    name: 'Pizza Calabresa',
    price: 0,
    status: 'ACTIVE',
    categoryId: 'c3',
    categoryName: 'Pizzas',
  },
]

const PUBLISH_RESULT: IfoodCatalogPublishResult = {
  publishedProducts: 1,
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
      name: 'Coca-Cola Lata',
      externalCode: null,
      outcome: 'SKIPPED',
      reason: 'Produto sem preço cadastrado',
    },
    {
      productId: 'p3',
      name: 'Pizza Calabresa',
      externalCode: null,
      outcome: 'FAILED',
      reason: 'Nome acima de 100 caracteres',
    },
  ],
}

const ACCEPTED: IfoodCatalogBatchAccepted = {
  batchId: 'batch-1',
  requested: 1,
  skipped: [{ productId: 'p2', reason: 'Produto ainda não publicado' }],
}

const PROCESSING: IfoodCatalogBatchStatus = {
  batchId: 'batch-1',
  status: 'PROCESSING',
  successCount: 0,
  failureCount: 0,
  results: [],
}

const COMPLETED: IfoodCatalogBatchStatus = {
  batchId: 'batch-1',
  status: 'COMPLETED',
  successCount: 1,
  failureCount: 0,
  results: [{ resourceId: 'p1', result: 'SUCCESS' }],
}

enableAutoUnmount(afterEach)

describe('IfoodCatalogPublishModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function mountModal() {
    const store = useProductStore()
    vi.spyOn(store, 'fetchAll').mockResolvedValue()
    store.items = [...PRODUCTS]
    return mount(IfoodCatalogPublishModal)
  }

  async function publish(wrapper: ReturnType<typeof mountModal>) {
    await wrapper.find('[data-testid="ifood-publish-start"]').trigger('click')
    await flushPromises()
  }

  it('lista os produtos com nome e preço e deixa claro o canal Cardápio Digital', async () => {
    const wrapper = mountModal()
    await flushPromises()

    const list = wrapper.find('[data-testid="ifood-publish-products"]')
    expect(list.exists()).toBe(true)
    expect(list.text()).toContain('X-Burger')
    expect(list.text()).toContain('Coca-Cola Lata')
    expect(list.text()).toContain('25,00')
    expect(wrapper.text()).toContain('Cardápio Digital')
    expect(wrapper.text()).toContain('WHITELABEL')
    expect(mockedService.publishCatalog).not.toHaveBeenCalled()
  })

  it('publica todos os produtos quando nada é desmarcado', async () => {
    mockedService.publishCatalog.mockResolvedValue(PUBLISH_RESULT)
    const wrapper = mountModal()
    await flushPromises()

    await publish(wrapper)

    expect(mockedService.publishCatalog).toHaveBeenCalledWith(['p1', 'p2', 'p3'])
  })

  it('publica apenas os produtos selecionados', async () => {
    mockedService.publishCatalog.mockResolvedValue(PUBLISH_RESULT)
    const wrapper = mountModal()
    await flushPromises()

    await wrapper.find('[data-testid="ifood-publish-select-none"]').trigger('click')
    await wrapper.find('[data-testid="ifood-publish-check-p2"]').setValue(true)
    await publish(wrapper)

    expect(mockedService.publishCatalog).toHaveBeenCalledWith(['p2'])
  })

  it('publicação com sucesso mostra contadores e o resultado de cada item', async () => {
    mockedService.publishCatalog.mockResolvedValue(PUBLISH_RESULT)
    const wrapper = mountModal()
    await flushPromises()

    await publish(wrapper)

    const result = wrapper.find('[data-testid="ifood-publish-result"]')
    expect(result.exists()).toBe(true)
    expect(result.text()).toContain('X-Burger')
    expect(result.text()).toContain('BURGER_001')
    expect(result.text()).toContain('Publicados')
    expect(wrapper.emitted('published')).toBeTruthy()
    expect(wrapper.emitted('published')![0]).toEqual([PUBLISH_RESULT])
  })

  it('itens ignorados e com falha exibem o motivo', async () => {
    mockedService.publishCatalog.mockResolvedValue(PUBLISH_RESULT)
    const wrapper = mountModal()
    await flushPromises()

    await publish(wrapper)

    const issues = wrapper.find('[data-testid="ifood-publish-issues"]')
    expect(issues.text()).toContain('Produto sem preço cadastrado')
    expect(issues.text()).toContain('Nome acima de 100 caracteres')
  })

  it('separa ignorados de falhas nos contadores', async () => {
    mockedService.publishCatalog.mockResolvedValue(PUBLISH_RESULT)
    const wrapper = mountModal()
    await flushPromises()

    await publish(wrapper)

    // o backend agrega falhas em skippedProducts; a UI mostra 1 ignorado e 1 falha
    const counters = wrapper.findAll('[data-testid^="ifood-publish-count-"]')
    const byId = new Map(
      counters.map((c) => [c.attributes('data-testid'), c.text()] as const),
    )
    expect(byId.get('ifood-publish-count-published')).toContain('1')
    expect(byId.get('ifood-publish-count-skipped')).toContain('1')
    expect(byId.get('ifood-publish-count-failed')).toContain('1')
  })

  it('desabilita o botão enquanto publica', async () => {
    let resolvePublish: (r: IfoodCatalogPublishResult) => void
    mockedService.publishCatalog.mockImplementation(
      () => new Promise((resolve) => (resolvePublish = resolve)),
    )
    const wrapper = mountModal()
    await flushPromises()

    await wrapper.find('[data-testid="ifood-publish-start"]').trigger('click')

    expect(wrapper.find('[data-testid="ifood-publish-start"]').attributes('disabled')).toBeDefined()

    resolvePublish!(PUBLISH_RESULT)
    await flushPromises()
  })

  describe('erros', () => {
    const cases: { status: number; expected: string }[] = [
      { status: 409, expected: 'Conecte' },
      { status: 422, expected: 'inválidos' },
      { status: 404, expected: 'não encontrado' },
      { status: 503, expected: 'indisponível' },
    ]

    for (const { status, expected } of cases) {
      it(`mapeia ${status} para mensagem em pt-BR`, async () => {
        mockedService.publishCatalog.mockRejectedValue({ response: { status } })
        const wrapper = mountModal()
        await flushPromises()

        await publish(wrapper)

        expect(wrapper.find('[data-testid="ifood-publish-error"]').text()).toContain(expected)
        expect(wrapper.emitted('published')).toBeFalsy()
      })
    }

    it('mapeia erro desconhecido para mensagem genérica', async () => {
      mockedService.publishCatalog.mockRejectedValue(new Error('Network Error'))
      const wrapper = mountModal()
      await flushPromises()

      await publish(wrapper)

      expect(wrapper.find('[data-testid="ifood-publish-error"]').text()).toContain(
        'Tente novamente',
      )
    })
  })

  describe('operações em lote', () => {
    async function publishThen(wrapper: ReturnType<typeof mountModal>) {
      mockedService.publishCatalog.mockResolvedValue(PUBLISH_RESULT)
      await flushPromises()
      await publish(wrapper)
    }

    it('sincroniza preços dos produtos publicados e acompanha o lote até concluir', async () => {
      vi.useFakeTimers()
      mockedService.syncPrices.mockResolvedValue(ACCEPTED)
      mockedService.getBatch.mockResolvedValueOnce(PROCESSING).mockResolvedValue(COMPLETED)
      const wrapper = mountModal()
      await publishThen(wrapper)

      await wrapper.find('[data-testid="ifood-publish-sync-prices"]').trigger('click')
      await flushPromises()

      expect(mockedService.syncPrices).toHaveBeenCalledWith(['p1'])
      const batch = wrapper.find('[data-testid="ifood-publish-batch"]')
      expect(batch.text()).toContain('Produto ainda não publicado')

      await vi.advanceTimersByTimeAsync(2100)
      await flushPromises()

      const done = wrapper.find('[data-testid="ifood-publish-batch"]')
      expect(done.text()).toContain('1')
      expect(done.text()).toContain('Concluído')
      expect(mockedService.getBatch).toHaveBeenCalledTimes(2)

      // após concluir, o polling para
      await vi.advanceTimersByTimeAsync(10_000)
      expect(mockedService.getBatch).toHaveBeenCalledTimes(2)
    })

    it('pausa os produtos publicados enviando status UNAVAILABLE', async () => {
      mockedService.syncStatus.mockResolvedValue(ACCEPTED)
      mockedService.getBatch.mockResolvedValue(COMPLETED)
      const wrapper = mountModal()
      await publishThen(wrapper)

      await wrapper.find('[data-testid="ifood-publish-pause"]').trigger('click')
      await flushPromises()

      expect(mockedService.syncStatus).toHaveBeenCalledWith([
        { productId: 'p1', status: 'UNAVAILABLE' },
      ])
    })

    it('reativa os produtos publicados enviando status AVAILABLE', async () => {
      mockedService.syncStatus.mockResolvedValue(ACCEPTED)
      mockedService.getBatch.mockResolvedValue(COMPLETED)
      const wrapper = mountModal()
      await publishThen(wrapper)

      await wrapper.find('[data-testid="ifood-publish-resume"]').trigger('click')
      await flushPromises()

      expect(mockedService.syncStatus).toHaveBeenCalledWith([
        { productId: 'p1', status: 'AVAILABLE' },
      ])
    })

    it('lote sem batchId (nada enviado) não consulta o iFood e mostra os motivos', async () => {
      mockedService.syncPrices.mockResolvedValue({
        batchId: null,
        requested: 0,
        skipped: [{ productId: 'p1', reason: 'Produto sem código no iFood' }],
      })
      const wrapper = mountModal()
      await publishThen(wrapper)

      await wrapper.find('[data-testid="ifood-publish-sync-prices"]').trigger('click')
      await flushPromises()

      expect(mockedService.getBatch).not.toHaveBeenCalled()
      expect(wrapper.find('[data-testid="ifood-publish-batch"]').text()).toContain(
        'Produto sem código no iFood',
      )
    })

    it('erro no lote mostra mensagem em pt-BR', async () => {
      mockedService.syncPrices.mockRejectedValue({ response: { status: 503 } })
      const wrapper = mountModal()
      await publishThen(wrapper)

      await wrapper.find('[data-testid="ifood-publish-sync-prices"]').trigger('click')
      await flushPromises()

      expect(wrapper.find('[data-testid="ifood-publish-batch-error"]').text()).toContain(
        'indisponível',
      )
    })

    it('interrompe o polling ao desmontar o modal', async () => {
      vi.useFakeTimers()
      mockedService.syncPrices.mockResolvedValue(ACCEPTED)
      mockedService.getBatch.mockResolvedValue(PROCESSING)
      const wrapper = mountModal()
      await publishThen(wrapper)

      await wrapper.find('[data-testid="ifood-publish-sync-prices"]').trigger('click')
      await flushPromises()
      expect(mockedService.getBatch).toHaveBeenCalledTimes(1)

      wrapper.unmount()
      await vi.advanceTimersByTimeAsync(20_000)

      expect(mockedService.getBatch).toHaveBeenCalledTimes(1)
    })

    it('para de consultar o lote após o limite de tentativas', async () => {
      vi.useFakeTimers()
      mockedService.syncPrices.mockResolvedValue(ACCEPTED)
      mockedService.getBatch.mockResolvedValue(PROCESSING)
      const wrapper = mountModal()
      await publishThen(wrapper)

      await wrapper.find('[data-testid="ifood-publish-sync-prices"]').trigger('click')
      await flushPromises()

      await vi.advanceTimersByTimeAsync(120_000)

      expect(mockedService.getBatch.mock.calls.length).toBeLessThanOrEqual(10)
      expect(wrapper.find('[data-testid="ifood-publish-batch-error"]').exists()).toBe(true)
    })
  })

  it('botão cancelar emite close', async () => {
    const wrapper = mountModal()
    await flushPromises()

    await wrapper.find('[data-testid="ifood-publish-cancel"]').trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })
})
