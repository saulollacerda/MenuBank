import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'

import UIPeriodPicker from '@/design/UIPeriodPicker.vue'

describe('UIPeriodPicker', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-07-10T12:00:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function factory(props: {
    month: number
    year: number
    mode?: 'month' | 'custom'
    startDate?: string
    endDate?: string
  }) {
    return mount(UIPeriodPicker, { props, attachTo: document.body })
  }

  it('exibe o rótulo do período com mês e ano selecionados', () => {
    const wrapper = factory({ month: 7, year: 2026 })
    expect(wrapper.find('[data-testid="period-picker-toggle"]').text()).toContain('Julho 2026')
  })

  it('começa com o menu fechado e abre ao clicar no botão', async () => {
    const wrapper = factory({ month: 7, year: 2026 })
    expect(wrapper.find('[data-testid="period-picker-menu"]').exists()).toBe(false)
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    expect(wrapper.find('[data-testid="period-picker-menu"]').exists()).toBe(true)
  })

  it('seleciona um mês, emite update:month e fecha o menu', async () => {
    const wrapper = factory({ month: 7, year: 2026 })
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    await wrapper.find('[data-testid="period-month-3"]').trigger('click')
    expect(wrapper.emitted('update:month')).toEqual([[3]])
    expect(wrapper.find('[data-testid="period-picker-menu"]').exists()).toBe(false)
  })

  it('seleciona um ano e emite update:year sem fechar o menu', async () => {
    const wrapper = factory({ month: 7, year: 2026 })
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    await wrapper.find('[data-testid="period-year-2026"]').trigger('click')
    expect(wrapper.emitted('update:year')).toEqual([[2026]])
    expect(wrapper.find('[data-testid="period-picker-menu"]').exists()).toBe(true)
  })

  it('no ano corrente só oferece meses até o mês atual', async () => {
    const wrapper = factory({ month: 7, year: 2026 })
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    expect(wrapper.find('[data-testid="period-month-7"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="period-month-8"]').exists()).toBe(false)
  })

  it('emite update:month com o mês máximo quando o mês fica inválido para o ano', async () => {
    vi.setSystemTime(new Date('2027-03-05T12:00:00'))
    const wrapper = factory({ month: 12, year: 2026 })
    await wrapper.setProps({ year: 2027 })
    expect(wrapper.emitted('update:month')).toEqual([[3]])
  })

  it('fecha o menu ao clicar fora', async () => {
    const wrapper = factory({ month: 7, year: 2026 })
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    document.body.click()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-testid="period-picker-menu"]').exists()).toBe(false)
  })

  it('emite change no modo mês ao selecionar um mês', async () => {
    const wrapper = factory({ month: 7, year: 2026 })
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    await wrapper.find('[data-testid="period-month-3"]').trigger('click')
    expect(wrapper.emitted('change')).toEqual([[{ mode: 'month' }]])
  })

  it('emite change no modo mês ao selecionar um ano', async () => {
    const wrapper = factory({ month: 7, year: 2026 })
    await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
    await wrapper.find('[data-testid="period-year-2026"]').trigger('click')
    expect(wrapper.emitted('change')).toEqual([[{ mode: 'month' }]])
  })

  describe('intervalo personalizado', () => {
    async function openCustom() {
      const wrapper = factory({ month: 7, year: 2026 })
      await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
      await wrapper.find('[data-testid="period-tab-custom"]').trigger('click')
      return wrapper
    }

    it('oferece as abas de mês e de intervalo', async () => {
      const wrapper = factory({ month: 7, year: 2026 })
      await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
      expect(wrapper.find('[data-testid="period-tab-month"]').text()).toContain('Mês')
      expect(wrapper.find('[data-testid="period-tab-custom"]').text()).toContain('Período')
    })

    it('mostra o calendário do mês corrente ao abrir a aba de período', async () => {
      const wrapper = await openCustom()
      expect(wrapper.find('[data-testid="period-calendar"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="period-calendar-title"]').text()).toContain('Julho 2026')
    })

    it('navega entre meses do calendário', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-calendar-prev"]').trigger('click')
      expect(wrapper.find('[data-testid="period-calendar-title"]').text()).toContain('Junho 2026')
      await wrapper.find('[data-testid="period-calendar-next"]').trigger('click')
      expect(wrapper.find('[data-testid="period-calendar-title"]').text()).toContain('Julho 2026')
    })

    it('não permite avançar para meses futuros', async () => {
      const wrapper = await openCustom()
      const next = wrapper.find('[data-testid="period-calendar-next"]')
      expect(next.attributes('disabled')).toBeDefined()
    })

    it('desabilita dias futuros', async () => {
      const wrapper = await openCustom()
      expect(wrapper.find('[data-testid="period-day-2026-07-10"]').attributes('disabled')).toBeUndefined()
      expect(wrapper.find('[data-testid="period-day-2026-07-11"]').attributes('disabled')).toBeDefined()
    })

    it('seleciona um intervalo dentro do mesmo mês e emite change', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-day-2026-07-02"]').trigger('click')
      expect(wrapper.emitted('change')).toBeUndefined()
      await wrapper.find('[data-testid="period-day-2026-07-08"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-07-02', endDate: '2026-07-08' }],
      ])
      expect(wrapper.emitted('update:startDate')).toEqual([['2026-07-02']])
      expect(wrapper.emitted('update:endDate')).toEqual([['2026-07-08']])
      expect(wrapper.emitted('update:mode')).toEqual([['custom']])
    })

    it('seleciona um intervalo que atravessa meses', async () => {
      vi.setSystemTime(new Date('2026-12-20T12:00:00'))
      const wrapper = factory({ month: 12, year: 2026 })
      await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
      await wrapper.find('[data-testid="period-tab-custom"]').trigger('click')
      await wrapper.find('[data-testid="period-calendar-prev"]').trigger('click')
      await wrapper.find('[data-testid="period-day-2026-11-16"]').trigger('click')
      await wrapper.find('[data-testid="period-calendar-next"]').trigger('click')
      await wrapper.find('[data-testid="period-day-2026-12-07"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-11-16', endDate: '2026-12-07' }],
      ])
    })

    it('reinicia a seleção quando o segundo clique é anterior ao início', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-day-2026-07-08"]').trigger('click')
      await wrapper.find('[data-testid="period-day-2026-07-02"]').trigger('click')
      expect(wrapper.emitted('change')).toBeUndefined()
      expect(wrapper.find('[data-testid="period-range-hint"]').text()).toContain('02/07/2026')
      await wrapper.find('[data-testid="period-day-2026-07-05"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-07-02', endDate: '2026-07-05' }],
      ])
    })

    it('permite um intervalo de um único dia', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-day-2026-07-03"]').trigger('click')
      await wrapper.find('[data-testid="period-day-2026-07-03"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-07-03', endDate: '2026-07-03' }],
      ])
    })

    it('aplica o preset de hoje', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-preset-today"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-07-10', endDate: '2026-07-10' }],
      ])
    })

    it('aplica o preset de últimos 7 dias', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-preset-7d"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-07-04', endDate: '2026-07-10' }],
      ])
    })

    it('aplica o preset de últimos 30 dias', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-preset-30d"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-06-11', endDate: '2026-07-10' }],
      ])
    })

    it('aplica o preset deste mês', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-preset-month"]').trigger('click')
      expect(wrapper.emitted('change')).toEqual([
        [{ mode: 'custom', startDate: '2026-07-01', endDate: '2026-07-10' }],
      ])
    })

    it('fecha o menu após concluir o intervalo', async () => {
      const wrapper = await openCustom()
      await wrapper.find('[data-testid="period-day-2026-07-02"]').trigger('click')
      await wrapper.find('[data-testid="period-day-2026-07-08"]').trigger('click')
      expect(wrapper.find('[data-testid="period-picker-menu"]').exists()).toBe(false)
    })

    it('exibe o intervalo no rótulo do botão quando o modo é custom', () => {
      const wrapper = factory({
        month: 7,
        year: 2026,
        mode: 'custom',
        startDate: '2026-11-16',
        endDate: '2026-12-07',
      })
      expect(wrapper.find('[data-testid="period-picker-toggle"]').text()).toContain(
        '16/11/2026 a 07/12/2026',
      )
    })

    it('volta ao modo mês ao escolher um mês estando em custom', async () => {
      const wrapper = factory({
        month: 7,
        year: 2026,
        mode: 'custom',
        startDate: '2026-11-16',
        endDate: '2026-12-07',
      })
      await wrapper.find('[data-testid="period-picker-toggle"]').trigger('click')
      await wrapper.find('[data-testid="period-tab-month"]').trigger('click')
      await wrapper.find('[data-testid="period-month-5"]').trigger('click')
      expect(wrapper.emitted('update:mode')).toEqual([['month']])
      expect(wrapper.emitted('change')).toEqual([[{ mode: 'month' }]])
    })
  })
})
