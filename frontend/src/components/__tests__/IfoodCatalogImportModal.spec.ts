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
    { name: 'Coca-Cola Lata', externalCode: 'COKE_001', outcome: 'IMPORTED', reason: null },
    { name: 'X-Salada', externalCode: 'SALAD_001', outcome: 'LINKED', reason: null },
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

  it('lista de produtos importados começa recolhida e expande ao clicar em visualizar', async () => {
    mockedService.importCatalog.mockResolvedValue(RESULT)
    const wrapper = mountModal()
    vi.spyOn(useProductStore(), 'fetchAll').mockResolvedValue()
    vi.spyOn(useCategoryStore(), 'fetchAll').mockResolvedValue()

    await wrapper.find('[data-testid="ifood-import-start"]').trigger('click')
    await flushPromises()

    const toggle = wrapper.find('[data-testid="ifood-import-toggle-products"]')
    expect(toggle.text()).toContain('Visualizar produtos importados')
    expect(wrapper.find('[data-testid="ifood-import-products-list"]').exists()).toBe(false)

    await toggle.trigger('click')

    const list = wrapper.find('[data-testid="ifood-import-products-list"]')
    expect(list.exists()).toBe(true)
    expect(list.text()).toContain('X-Burger')
    expect(list.text()).toContain('Coca-Cola Lata')
    expect(list.text()).toContain('X-Salada')
    // ignorados não entram na lista de produtos importados
    expect(list.text()).not.toContain('Pizza Calabresa')
  })

  it('botão de visualizar recolhe a lista no segundo clique', async () => {
    mockedService.importCatalog.mockResolvedValue(RESULT)
    const wrapper = mountModal()
    vi.spyOn(useProductStore(), 'fetchAll').mockResolvedValue()
    vi.spyOn(useCategoryStore(), 'fetchAll').mockResolvedValue()

    await wrapper.find('[data-testid="ifood-import-start"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-testid="ifood-import-toggle-products"]').trigger('click')
    expect(wrapper.find('[data-testid="ifood-import-products-list"]').exists()).toBe(true)

    await wrapper.find('[data-testid="ifood-import-toggle-products"]').trigger('click')

    expect(wrapper.find('[data-testid="ifood-import-products-list"]').exists()).toBe(false)
  })

  it('sem produtos importados ou vinculados o botão de visualizar não aparece', async () => {
    mockedService.importCatalog.mockResolvedValue({
      importedProducts: 0,
      linkedProducts: 0,
      skippedProducts: 1,
      importedCategories: 0,
      linkedCategories: 0,
      items: [
        { name: 'Pizza Calabresa', externalCode: 'PIZZA_01', outcome: 'SKIPPED', reason: 'Item sem preço no catálogo' },
      ],
    })
    const wrapper = mountModal()
    vi.spyOn(useProductStore(), 'fetchAll').mockResolvedValue()
    vi.spyOn(useCategoryStore(), 'fetchAll').mockResolvedValue()

    await wrapper.find('[data-testid="ifood-import-start"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="ifood-import-toggle-products"]').exists()).toBe(false)
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

  it('clicar no backdrop não emite close', async () => {
    const wrapper = mountModal()

    await wrapper.get('.ui-modal-backdrop').trigger('click')

    expect(wrapper.emitted('close')).toBeFalsy()
  })
})
