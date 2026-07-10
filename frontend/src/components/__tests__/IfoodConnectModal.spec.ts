import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, enableAutoUnmount } from '@vue/test-utils'
import IfoodConnectModal from '@/components/IfoodConnectModal.vue'
import { ifoodAuthService } from '@/services/ifoodAuthService'
import { IFOOD_PENDING_KEY } from '@/composables/useIfoodConnectFlow'

vi.mock('vue-router', () => ({ onBeforeRouteLeave: vi.fn() }))

vi.mock('@/services/ifoodAuthService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/ifoodAuthService')>()
  return {
    ...original,
    ifoodAuthService: {
      status: vi.fn(),
      start: vi.fn(),
      connect: vi.fn(),
      revoke: vi.fn(),
    },
  }
})

const mockedService = vi.mocked(ifoodAuthService)

const START_RESPONSE = {
  userCode: 'HJLX-LPSQ',
  verificationUrl: 'https://portal.ifood.com.br/apps/code',
  verificationUrlComplete: 'https://portal.ifood.com.br/apps/code?c=HJLX-LPSQ',
  expiresIn: 600,
}

async function mountModal(props: { resume?: boolean } = {}) {
  const wrapper = mount(IfoodConnectModal, { props })
  await flushPromises()
  return wrapper
}

enableAutoUnmount(afterEach)

describe('IfoodConnectModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sessionStorage.clear()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-04T12:00:00Z'))
    mockedService.start.mockResolvedValue(START_RESPONSE)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('mount sem resume chama start e mostra código + countdown formatado', async () => {
    const wrapper = await mountModal()

    expect(mockedService.start).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="ifood-user-code"]').text()).toBe('HJLX-LPSQ')
    expect(wrapper.find('[data-testid="ifood-countdown"]').text()).toContain('10:00')
  })

  it('link do portal usa verificationUrlComplete com fallback para verificationUrl', async () => {
    const wrapper = await mountModal()
    expect(wrapper.find('[data-testid="ifood-portal-link"]').attributes('href')).toBe(
      START_RESPONSE.verificationUrlComplete,
    )

    mockedService.start.mockResolvedValue({ ...START_RESPONSE, verificationUrlComplete: '' })
    const fallback = await mountModal()
    expect(fallback.find('[data-testid="ifood-portal-link"]').attributes('href')).toBe(
      START_RESPONSE.verificationUrl,
    )
  })

  it('botão copiar escreve no clipboard e mostra "Copiado!" temporariamente', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true })
    const wrapper = await mountModal()

    await wrapper.find('[data-testid="ifood-copy-button"]').trigger('click')
    await flushPromises()

    expect(writeText).toHaveBeenCalledWith('HJLX-LPSQ')
    expect(wrapper.find('[data-testid="ifood-copy-button"]').text()).toContain('Copiado!')

    await vi.advanceTimersByTimeAsync(2500)
    expect(wrapper.find('[data-testid="ifood-copy-button"]').text()).not.toContain('Copiado!')
  })

  it('progresso avança: copiar → passo 2, preencher código → passo 3', async () => {
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
      configurable: true,
    })
    const wrapper = await mountModal()
    expect(wrapper.find('[data-testid="ifood-step-chip-1"]').attributes('data-state')).toBe('active')

    await wrapper.find('[data-testid="ifood-copy-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="ifood-step-chip-2"]').attributes('data-state')).toBe('active')
    expect(wrapper.find('[data-testid="ifood-step-chip-1"]').attributes('data-state')).toBe('done')

    await wrapper.find('[data-testid="ifood-auth-input"]').setValue('abc')
    expect(wrapper.find('[data-testid="ifood-step-chip-3"]').attributes('data-state')).toBe('active')
  })

  it('countdown fica vermelho com 60s ou menos restantes', async () => {
    mockedService.start.mockResolvedValue({ ...START_RESPONSE, expiresIn: 90 })
    const wrapper = await mountModal()
    const countdown = () => wrapper.find('[data-testid="ifood-countdown"]')
    expect(countdown().attributes('data-urgent')).toBe('false')

    await vi.advanceTimersByTimeAsync(31_000) // restam 59s
    expect(countdown().attributes('data-urgent')).toBe('true')
  })

  it('ao expirar mostra estado expirado com regenerar e input ainda habilitado', async () => {
    mockedService.start.mockResolvedValue({ ...START_RESPONSE, expiresIn: 30 })
    const wrapper = await mountModal()

    await vi.advanceTimersByTimeAsync(31_000)

    expect(wrapper.find('[data-testid="ifood-expired-state"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="ifood-auth-input"]').attributes('disabled')).toBeUndefined()

    await wrapper.find('[data-testid="ifood-regenerate-button"]').trigger('click')
    await flushPromises()
    expect(mockedService.start).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-testid="ifood-user-code"]').exists()).toBe(true)
  })

  it('connect com sucesso mostra estado de sucesso, emite connected e Fechar emite close', async () => {
    mockedService.connect.mockResolvedValue()
    const wrapper = await mountModal()

    await wrapper.find('[data-testid="ifood-auth-input"]').setValue('auth-123')
    await wrapper.find('[data-testid="ifood-confirm-button"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="ifood-done-state"]').text()).toContain('iFood conectado')
    expect(wrapper.emitted('connected')).toHaveLength(1)
    expect(wrapper.emitted('close')).toBeUndefined()
    expect(sessionStorage.getItem(IFOOD_PENDING_KEY)).toBeNull()

    await wrapper.find('[data-testid="ifood-done-close-button"]').trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('connect 409 mostra orientação de novo código; 400 mostra código inválido', async () => {
    const wrapper = await mountModal()
    await wrapper.find('[data-testid="ifood-auth-input"]').setValue('auth-123')

    mockedService.connect.mockRejectedValue({ response: { status: 409 } })
    await wrapper.find('[data-testid="ifood-confirm-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="ifood-error"]').text()).toContain('novo código')

    mockedService.connect.mockRejectedValue({ response: { status: 400 } })
    await wrapper.find('[data-testid="ifood-confirm-button"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="ifood-error"]').text()).toContain('inválido')
  })

  it('tentar fechar no meio do fluxo pede confirmação; continuar volta; sair fecha e limpa', async () => {
    const wrapper = await mountModal()

    await wrapper.find('[data-testid="ifood-cancel-button"]').trigger('click')
    expect(wrapper.find('[data-testid="ifood-cancel-confirm"]').text()).toContain('autorização pendente')
    expect(wrapper.emitted('close')).toBeUndefined()

    await wrapper.find('[data-testid="ifood-cancel-continue"]').trigger('click')
    expect(wrapper.find('[data-testid="ifood-cancel-confirm"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="ifood-user-code"]').exists()).toBe(true)

    await wrapper.find('[data-testid="ifood-cancel-button"]').trigger('click')
    await wrapper.find('[data-testid="ifood-cancel-leave"]').trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(1)
    expect(sessionStorage.getItem(IFOOD_PENDING_KEY)).toBeNull()
  })

  it('fechar no estado de sucesso não pede confirmação', async () => {
    mockedService.connect.mockResolvedValue()
    const wrapper = await mountModal()
    await wrapper.find('[data-testid="ifood-auth-input"]').setValue('auth-123')
    await wrapper.find('[data-testid="ifood-confirm-button"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="ifood-done-close-button"]').trigger('click')

    expect(wrapper.find('[data-testid="ifood-cancel-confirm"]').exists()).toBe(false)
    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('resume monta a partir do sessionStorage sem chamar start', async () => {
    sessionStorage.setItem(
      IFOOD_PENDING_KEY,
      JSON.stringify({
        v: 1,
        userCode: 'OLD-CODE',
        verificationUrl: START_RESPONSE.verificationUrl,
        verificationUrlComplete: START_RESPONSE.verificationUrlComplete,
        expiresAt: Date.now() + 120_000,
      }),
    )

    const wrapper = await mountModal({ resume: true })

    expect(mockedService.start).not.toHaveBeenCalled()
    expect(wrapper.find('[data-testid="ifood-user-code"]').text()).toBe('OLD-CODE')
    expect(wrapper.find('[data-testid="ifood-countdown"]').text()).toContain('02:00')
  })

  it('beforeunload é bloqueado durante o fluxo e liberado após conectar', async () => {
    mockedService.connect.mockResolvedValue()
    const wrapper = await mountModal()

    const midFlow = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(midFlow)
    expect(midFlow.defaultPrevented).toBe(true)

    await wrapper.find('[data-testid="ifood-auth-input"]').setValue('auth-123')
    await wrapper.find('[data-testid="ifood-confirm-button"]').trigger('click')
    await flushPromises()

    const afterDone = new Event('beforeunload', { cancelable: true })
    window.dispatchEvent(afterDone)
    expect(afterDone.defaultPrevented).toBe(false)
  })
})
