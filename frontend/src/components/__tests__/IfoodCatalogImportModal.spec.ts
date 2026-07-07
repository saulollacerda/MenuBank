import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, enableAutoUnmount } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import IfoodCatalogImportModal from '@/components/IfoodCatalogImportModal.vue'
import { ifoodCatalogService, type IfoodCatalogImportResult } from '@/services/ifoodCatalogService'
import { useProductStore } from '@/stores/productStore'
import { useCategoryStore } from '@/stores/categoryStore'

vi.mock('@/services/ifoodCatalogService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/ifoodCatalogService')>()
  return {
    ...original,
    ifoodCatalogService: {
      importCatalog: vi.fn(),
    },
  }
})

const mockedService = vi.mocked(ifoodCatalogService)

const RESULT: IfoodCatalogImportResult = {
  importedProducts: 3,
  linkedProducts: 2,
  skippedProducts: 1,
  importedCategories: 2,
  linkedCategories: 1,
  items: [
    { name: 'X-Burger', externalCode: 'BURGER_001', outcome: 'IMPORTED', reason: null },
    { name: 'Pizza Calabresa', externalCode: 'PIZZA_01', outcome: 'SKIPPED', reason: 'Item sem preço no catálogo' },
  ],
}

enableAutoUnmount(afterEach)

describe('IfoodCatalogImportModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  function mountModal() {
    return mount(IfoodCatalogImportModal)
  }

  it('estado inicial mostra explicação e botão de importar, sem chamar o serviço', () => {
    const wrapper = mountModal()

    expect(wrapper.find('[data-testid="ifood-import-start"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('não altera')
    expect(mockedService.importCatalog).not.toHaveBeenCalled()
  })

  it('importação com sucesso mostra contadores, motivo dos ignorados e emite imported', async () => {
    mockedService.importCatalog.mockResolvedValue(RESULT)
    const wrapper = mountModal()
    const productStore = useProductStore()
    const categoryStore = useCategoryStore()
    const productRefresh = vi.spyOn(productStore, 'fetchAll').mockResolvedValue()
    const categoryRefresh = vi.spyOn(categoryStore, 'fetchAll').mockResolvedValue()

    await wrapper.find('[data-testid="ifood-import-start"]').trigger('click')
    await flushPromises()

    const result = wrapper.find('[data-testid="ifood-import-result"]')
    expect(result.exists()).toBe(true)
    expect(result.text()).toContain('3')
    expect(result.text()).toContain('2')
    expect(result.text()).toContain('1')
    expect(wrapper.text()).toContain('Item sem preço no catálogo')
    expect(wrapper.emitted('imported')).toBeTruthy()
    expect(wrapper.emitted('imported')![0]).toEqual([RESULT])
    expect(productRefresh).toHaveBeenCalledWith(true)
    expect(categoryRefresh).toHaveBeenCalledWith(true)
  })

  it('desabilita o botão enquanto importa', async () => {
    let resolveImport: (r: IfoodCatalogImportResult) => void
    mockedService.importCatalog.mockImplementation(
      () => new Promise((resolve) => (resolveImport = resolve)),
    )
    const wrapper = mountModal()

    await wrapper.find('[data-testid="ifood-import-start"]').trigger('click')

    expect(wrapper.find('[data-testid="ifood-import-start"]').attributes('disabled')).toBeDefined()

    resolveImport!(RESULT)
    await flushPromises()
  })

  it('falha mostra mensagem de erro em pt-BR e não emite imported', async () => {
    mockedService.importCatalog.mockRejectedValue({ response: { status: 409 } })
    const wrapper = mountModal()

    await wrapper.find('[data-testid="ifood-import-start"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="ifood-import-error"]').text()).toContain('Conecte')
    expect(wrapper.emitted('imported')).toBeFalsy()
  })

  it('botão fechar emite close', async () => {
    const wrapper = mountModal()

    await wrapper.find('[data-testid="ifood-import-cancel"]').trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })
})
