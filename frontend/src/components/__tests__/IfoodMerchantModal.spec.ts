import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises, enableAutoUnmount } from '@vue/test-utils'
import IfoodMerchantModal from '@/components/IfoodMerchantModal.vue'
import {
  ifoodMerchantService,
  type MerchantDetails,
  type MerchantStatus,
  type MerchantInterruption,
  type OpeningHours,
} from '@/services/ifoodMerchantService'

vi.mock('@/services/ifoodMerchantService', async (importOriginal) => {
  const original = await importOriginal<typeof import('@/services/ifoodMerchantService')>()
  return {
    ...original,
    ifoodMerchantService: {
      getDetails: vi.fn(),
      getStatus: vi.fn(),
      getInterruptions: vi.fn(),
      createInterruption: vi.fn(),
      deleteInterruption: vi.fn(),
      getOpeningHours: vi.fn(),
      updateOpeningHours: vi.fn(),
    },
  }
})

const mockedService = vi.mocked(ifoodMerchantService)

const DETAILS: MerchantDetails = {
  id: 'm-1',
  name: 'Restaurante do Zé',
  corporateName: 'Zé Alimentos LTDA',
}

const STATUS: MerchantStatus[] = [
  {
    operation: 'DELIVERY',
    salesChannel: 'IFOOD',
    available: true,
    state: 'OK',
    message: null,
    validations: [],
  },
  {
    operation: 'TAKEOUT',
    salesChannel: 'IFOOD',
    available: false,
    state: 'CLOSED',
    message: { title: 'Loja fechada', subtitle: 'Fora do horário de funcionamento' },
    validations: [
      {
        id: 'v-1',
        code: 'opening-hours',
        state: 'CLOSED',
        message: { title: 'Sem turno agora', subtitle: 'Nenhum horário configurado para hoje' },
      },
    ],
  },
]

const INTERRUPTIONS: MerchantInterruption[] = [
  {
    id: 'i-1',
    description: 'Manutenção da cozinha',
    start: '2026-07-20T12:00:00',
    end: '2026-07-20T14:00:00',
  },
]

const HOURS: OpeningHours = {
  shifts: [
    { id: 's-1', dayOfWeek: 'MONDAY', start: '08:00:00', duration: 600 },
    { id: 's-2', dayOfWeek: 'FRIDAY', start: '18:00:00', duration: 300 },
  ],
}

enableAutoUnmount(afterEach)

describe('IfoodMerchantModal', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedService.getDetails.mockResolvedValue(DETAILS)
    mockedService.getStatus.mockResolvedValue(STATUS)
    mockedService.getInterruptions.mockResolvedValue(INTERRUPTIONS)
    mockedService.getOpeningHours.mockResolvedValue(HOURS)
  })

  async function mountModal() {
    const wrapper = mount(IfoodMerchantModal)
    await flushPromises()
    return wrapper
  }

  it('carrega detalhes e status com estados traduzidos', async () => {
    const wrapper = await mountModal()

    expect(mockedService.getDetails).toHaveBeenCalled()
    expect(mockedService.getStatus).toHaveBeenCalled()
    expect(wrapper.find('[data-testid="merchant-store-name"]').text()).toContain('Restaurante do Zé')
    const statusText = wrapper.find('[data-testid="merchant-status-list"]').text()
    expect(statusText).toContain('Aberta')
    expect(statusText).toContain('Fechada')
    expect(statusText).toContain('Loja fechada')
    expect(statusText).toContain('Sem turno agora')
  })

  it('botão Atualizar recarrega o status', async () => {
    const wrapper = await mountModal()
    mockedService.getStatus.mockClear()

    await wrapper.find('[data-testid="merchant-status-refresh"]').trigger('click')
    await flushPromises()

    expect(mockedService.getStatus).toHaveBeenCalledTimes(1)
  })

  it('lista as pausas existentes formatadas', async () => {
    const wrapper = await mountModal()

    await wrapper.find('[data-testid="tab-pauses"]').trigger('click')
    await flushPromises()

    const rows = wrapper.findAll('[data-testid="interruption-row"]')
    expect(rows).toHaveLength(1)
    expect(rows[0]!.text()).toContain('Manutenção da cozinha')
  })

  it('cria uma pausa chamando o serviço com os valores do formulário', async () => {
    mockedService.createInterruption.mockResolvedValue({
      id: 'i-2',
      description: 'Almoço',
      start: '2026-07-21T12:00',
      end: '2026-07-21T13:00',
    })
    const wrapper = await mountModal()
    await wrapper.find('[data-testid="tab-pauses"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="interruption-description"]').setValue('Almoço')
    await wrapper.find('[data-testid="interruption-start"]').setValue('2026-07-21T12:00')
    await wrapper.find('[data-testid="interruption-end"]').setValue('2026-07-21T13:00')
    await wrapper.find('[data-testid="interruption-submit"]').trigger('click')
    await flushPromises()

    expect(mockedService.createInterruption).toHaveBeenCalledWith({
      description: 'Almoço',
      start: '2026-07-21T12:00',
      end: '2026-07-21T13:00',
    })
  })

  it('mostra a mensagem de sobreposição (409) ao criar pausa', async () => {
    mockedService.createInterruption.mockRejectedValue({
      response: { status: 409, data: { detail: 'Já existe uma pausa nesse período. Ajuste os horários.' } },
    })
    const wrapper = await mountModal()
    await wrapper.find('[data-testid="tab-pauses"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="interruption-description"]').setValue('Almoço')
    await wrapper.find('[data-testid="interruption-start"]').setValue('2026-07-21T12:00')
    await wrapper.find('[data-testid="interruption-end"]').setValue('2026-07-21T13:00')
    await wrapper.find('[data-testid="interruption-submit"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="interruption-error"]').text()).toContain(
      'Já existe uma pausa nesse período',
    )
  })

  it('remove uma pausa após confirmação', async () => {
    mockedService.deleteInterruption.mockResolvedValue()
    const wrapper = await mountModal()
    await wrapper.find('[data-testid="tab-pauses"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="interruption-delete"]').trigger('click')
    // confirm step must appear before the service is called
    expect(mockedService.deleteInterruption).not.toHaveBeenCalled()

    await wrapper.find('[data-testid="interruption-confirm-delete"]').trigger('click')
    await flushPromises()

    expect(mockedService.deleteInterruption).toHaveBeenCalledWith('i-1')
  })

  it('renderiza os horários agrupados por dia da semana', async () => {
    const wrapper = await mountModal()

    await wrapper.find('[data-testid="tab-hours"]').trigger('click')
    await flushPromises()

    const hoursText = wrapper.find('[data-testid="hours-list"]').text()
    expect(hoursText).toContain('Segunda-feira')
    expect(hoursText).toContain('Sexta-feira')
    // start 08:00 + 600min -> ends 18:00
    expect(hoursText).toContain('18:00')
  })

  it('salva os horários editados via PUT', async () => {
    mockedService.updateOpeningHours.mockResolvedValue(HOURS)
    const wrapper = await mountModal()
    await wrapper.find('[data-testid="tab-hours"]').trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="hours-save"]').trigger('click')
    await flushPromises()

    expect(mockedService.updateOpeningHours).toHaveBeenCalledWith([
      { dayOfWeek: 'MONDAY', start: '08:00:00', duration: 600 },
      { dayOfWeek: 'FRIDAY', start: '18:00:00', duration: 300 },
    ])
    expect(wrapper.find('[data-testid="hours-success"]').exists()).toBe(true)
  })

  it('surface a mensagem de erro quando o status falha (não conectado)', async () => {
    mockedService.getStatus.mockRejectedValue({
      response: { status: 409, data: { detail: 'Conecte sua conta do iFood para gerenciar sua loja.' } },
    })
    const wrapper = await mountModal()

    expect(wrapper.find('[data-testid="merchant-status-error"]').text()).toContain(
      'Conecte sua conta do iFood',
    )
  })
})
