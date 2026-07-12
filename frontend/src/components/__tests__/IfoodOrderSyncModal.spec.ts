import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, enableAutoUnmount } from '@vue/test-utils'
import IfoodOrderSyncModal from '@/components/IfoodOrderSyncModal.vue'
import { ifoodAuthService, type IfoodStatusResponse } from '@/services/ifoodAuthService'

vi.mock('@/services/ifoodAuthService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/ifoodAuthService')>()
  return {
    ...original,
    ifoodAuthService: {
      setOrderSync: vi.fn(),
    },
  }
})

const mockedService = vi.mocked(ifoodAuthService)

const ENABLED_STATUS: IfoodStatusResponse = {
  connected: true,
  catalogImportedAt: '2026-07-01T10:00:00',
  orderSyncEnabled: true,
  connectionEnabled: true,
}

enableAutoUnmount(afterEach)

describe('IfoodOrderSyncModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  function mountModal(props: { enabled: boolean; catalogImported: boolean }) {
    return mount(IfoodOrderSyncModal, { props })
  }

  it('sem catálogo importado mostra aviso soft-gate mas permite ativar', () => {
    const wrapper = mountModal({ enabled: false, catalogImported: false })

    expect(wrapper.find('[data-testid="ifood-sync-warning"]').text()).toContain('ignorados')
    expect(
      wrapper.find('[data-testid="ifood-sync-toggle"]').attributes('disabled'),
    ).toBeUndefined()
  })

  it('com catálogo importado não mostra o aviso', () => {
    const wrapper = mountModal({ enabled: false, catalogImported: true })

    expect(wrapper.find('[data-testid="ifood-sync-warning"]').exists()).toBe(false)
  })

  it('desativado: botão Ativar chama setOrderSync(true) e emite updated', async () => {
    mockedService.setOrderSync.mockResolvedValue(ENABLED_STATUS)
    const wrapper = mountModal({ enabled: false, catalogImported: true })

    const toggle = wrapper.find('[data-testid="ifood-sync-toggle"]')
    expect(toggle.text()).toContain('Ativar')
    await toggle.trigger('click')
    await flushPromises()

    expect(mockedService.setOrderSync).toHaveBeenCalledWith(true)
    expect(wrapper.emitted('updated')![0]).toEqual([ENABLED_STATUS])
  })

  it('ativado: botão Desativar chama setOrderSync(false)', async () => {
    mockedService.setOrderSync.mockResolvedValue({ ...ENABLED_STATUS, orderSyncEnabled: false })
    const wrapper = mountModal({ enabled: true, catalogImported: true })

    const toggle = wrapper.find('[data-testid="ifood-sync-toggle"]')
    expect(toggle.text()).toContain('Desativar')
    await toggle.trigger('click')
    await flushPromises()

    expect(mockedService.setOrderSync).toHaveBeenCalledWith(false)
    expect(wrapper.emitted('updated')).toBeTruthy()
  })

  it('falha mostra mensagem de erro em pt-BR e não emite updated', async () => {
    mockedService.setOrderSync.mockRejectedValue(new Error('boom'))
    const wrapper = mountModal({ enabled: false, catalogImported: true })

    await wrapper.find('[data-testid="ifood-sync-toggle"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="ifood-sync-error"]').text()).toContain('Tente novamente')
    expect(wrapper.emitted('updated')).toBeFalsy()
  })

  it('botão cancelar emite close', async () => {
    const wrapper = mountModal({ enabled: false, catalogImported: true })

    await wrapper.find('[data-testid="ifood-sync-cancel"]').trigger('click')

    expect(wrapper.emitted('close')).toBeTruthy()
  })
})
