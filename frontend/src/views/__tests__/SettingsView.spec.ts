import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, enableAutoUnmount } from '@vue/test-utils'
import SettingsView from '@/views/SettingsView.vue'
import IfoodConnectModal from '@/components/IfoodConnectModal.vue'
import IfoodCatalogImportModal from '@/components/IfoodCatalogImportModal.vue'
import IfoodOrderSyncModal from '@/components/IfoodOrderSyncModal.vue'
import { ifoodAuthService, type IfoodStatusResponse } from '@/services/ifoodAuthService'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { section: 'ints' } }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
  onBeforeRouteLeave: vi.fn(),
}))

let mockUser: { anotaAiApiKey: string | null; openingHours: unknown[] }

vi.mock('@/stores/authStore', () => ({
  useAuthStore: () => ({
    currentUser: mockUser,
    restaurantName: 'Test Burguer',
    loading: false,
    error: null,
    fetchCurrentUser: vi.fn(async () => mockUser),
    updateOpeningHours: vi.fn(),
    updateAnotaAIKey: vi.fn(),
  }),
}))

let anotaAIStoreMock: any

vi.mock('@/stores/anotaAIStore', () => ({
  useAnotaAIStore: () => anotaAIStoreMock,
}))

vi.mock('@/stores/notificationStore', () => ({
  useNotificationStore: () => ({ refreshCount: vi.fn() }),
}))

vi.mock('@/services/ifoodAuthService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/ifoodAuthService')>()
  return {
    ...original,
    ifoodAuthService: {
      status: vi.fn(),
      revoke: vi.fn(),
    },
  }
})

const mockedService = vi.mocked(ifoodAuthService)

const STUBS = {
  IfoodConnectModal: true,
  IfoodCatalogImportModal: true,
  IfoodOrderSyncModal: true,
}

function statusOf(overrides: Partial<IfoodStatusResponse> = {}): IfoodStatusResponse {
  return { connected: false, catalogImportedAt: null, orderSyncEnabled: false, ...overrides }
}

async function mountView(status: IfoodStatusResponse) {
  mockedService.status.mockResolvedValue(status)
  const wrapper = mount(SettingsView, { global: { stubs: STUBS } })
  await flushPromises()
  return wrapper
}

enableAutoUnmount(afterEach)

beforeEach(() => {
  mockUser = { anotaAiApiKey: null, openingHours: [] }
  anotaAIStoreMock = {
    syncingOrders: false,
    lastResult: null,
    error: null,
    syncOrders: vi.fn(async () => ({ ordersImported: 2, ordersSkipped: 1 })),
    clearResult: vi.fn(),
  }
})

describe('SettingsView — checklist iFood', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
  })

  it('mostra as 3 etapas com estado correto quando tudo está ativo', async () => {
    const wrapper = await mountView(
      statusOf({ connected: true, catalogImportedAt: '2026-07-01T10:00:00', orderSyncEnabled: true }),
    )

    expect(wrapper.find('[data-testid="ifood-stage-connect"]').text()).toContain('Conectado')
    expect(wrapper.find('[data-testid="ifood-stage-catalog"]').text()).toContain('Importado')
    expect(wrapper.find('[data-testid="ifood-stage-sync"]').text()).toContain('Ativa')
  })

  it('desconectado: etapas 2 e 3 ficam pendentes com ações desabilitadas', async () => {
    const wrapper = await mountView(statusOf())

    expect(wrapper.find('[data-testid="ifood-stage-connect"]').text()).toContain('Pendente')
    expect(wrapper.find('[data-testid="ifood-stage-catalog"]').text()).toContain('Pendente')
    expect(wrapper.find('[data-testid="ifood-stage-sync"]').text()).toContain('Inativa')
    expect(
      wrapper.find('[data-testid="ifood-stage-catalog-action"]').attributes('disabled'),
    ).toBeDefined()
    expect(
      wrapper.find('[data-testid="ifood-stage-sync-action"]').attributes('disabled'),
    ).toBeDefined()
  })

  it('clicar na ação da etapa 1 abre o modal de conexão', async () => {
    const wrapper = await mountView(statusOf())

    await wrapper.find('[data-testid="ifood-stage-connect-action"]').trigger('click')

    expect(wrapper.findComponent(IfoodConnectModal).exists()).toBe(true)
  })

  it('clicar na ação da etapa 2 abre o modal de importação de cardápio', async () => {
    const wrapper = await mountView(statusOf({ connected: true }))

    await wrapper.find('[data-testid="ifood-stage-catalog-action"]').trigger('click')

    expect(wrapper.findComponent(IfoodCatalogImportModal).exists()).toBe(true)
  })

  it('clicar na ação da etapa 3 abre o modal de sincronia', async () => {
    const wrapper = await mountView(statusOf({ connected: true }))

    await wrapper.find('[data-testid="ifood-stage-sync-action"]').trigger('click')

    expect(wrapper.findComponent(IfoodOrderSyncModal).exists()).toBe(true)
  })

  it('evento updated do modal de sincronia atualiza o estado da etapa 3', async () => {
    const wrapper = await mountView(statusOf({ connected: true }))
    await wrapper.find('[data-testid="ifood-stage-sync-action"]').trigger('click')

    wrapper.findComponent(IfoodOrderSyncModal).vm.$emit(
      'updated',
      statusOf({ connected: true, orderSyncEnabled: true }),
    )
    await flushPromises()

    expect(wrapper.find('[data-testid="ifood-stage-sync"]').text()).toContain('Ativa')
  })

  it('importação concluída recarrega o status do checklist', async () => {
    const wrapper = await mountView(statusOf({ connected: true }))
    await wrapper.find('[data-testid="ifood-stage-catalog-action"]').trigger('click')
    mockedService.status.mockResolvedValue(
      statusOf({ connected: true, catalogImportedAt: '2026-07-06T09:00:00' }),
    )

    wrapper.findComponent(IfoodCatalogImportModal).vm.$emit('imported', {
      importedProducts: 1, linkedProducts: 0, skippedProducts: 0,
      importedCategories: 1, linkedCategories: 0, items: [],
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="ifood-stage-catalog"]').text()).toContain('Importado')
  })

  it('sincronia ativa sem catálogo importado mostra indicador de aviso na etapa 3', async () => {
    const wrapper = await mountView(
      statusOf({ connected: true, catalogImportedAt: null, orderSyncEnabled: true }),
    )

    expect(wrapper.find('[data-testid="ifood-stage-sync-warning"]').exists()).toBe(true)
  })
})

describe('SettingsView — checklist Anota.AI', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
  })

  it('sem token: etapa 1 pendente e importação manual desabilitada', async () => {
    const wrapper = await mountView(statusOf())

    expect(wrapper.find('[data-testid="anotaai-stage-connect"]').text()).toContain('Pendente')
    expect(wrapper.find('[data-testid="anotaai-stage-hours"]').text()).toContain('Pendente')
    expect(
      wrapper.find('[data-testid="anotaai-stage-import-action"]').attributes('disabled'),
    ).toBeDefined()
  })

  it('com token e horários: etapas conectada e configurada, importação habilitada', async () => {
    mockUser = {
      anotaAiApiKey: 'token-123',
      openingHours: [{ dayOfWeek: 'MONDAY', openTime: '11:00', closeTime: '22:00', closed: false }],
    }
    const wrapper = await mountView(statusOf())

    expect(wrapper.find('[data-testid="anotaai-stage-connect"]').text()).toContain('Conectado')
    expect(wrapper.find('[data-testid="anotaai-stage-hours"]').text()).toContain('Configurado')
    expect(
      wrapper.find('[data-testid="anotaai-stage-import-action"]').attributes('disabled'),
    ).toBeUndefined()
  })

  it('ação da etapa de horários leva à seção de horários de funcionamento', async () => {
    const wrapper = await mountView(statusOf())

    await wrapper.find('[data-testid="anotaai-stage-hours-action"]').trigger('click')

    expect(wrapper.text()).toContain('Horários de funcionamento')
  })

  it('importar agora chama a sincronização de pedidos do Anota.AI', async () => {
    mockUser = { anotaAiApiKey: 'token-123', openingHours: [] }
    const wrapper = await mountView(statusOf())

    await wrapper.find('[data-testid="anotaai-stage-import-action"]').trigger('click')

    expect(anotaAIStoreMock.syncOrders).toHaveBeenCalled()
  })

  it('exibe o resultado da última importação dentro do card', async () => {
    mockUser = { anotaAiApiKey: 'token-123', openingHours: [] }
    anotaAIStoreMock.lastResult = { ordersImported: 3, ordersSkipped: 2, missingIngredientNames: [] }
    const wrapper = await mountView(statusOf())

    expect(wrapper.text()).toContain('3 pedido(s) importado(s)')
    expect(wrapper.text()).toContain('2 já existente(s)')
  })
})
