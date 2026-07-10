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

  function factory(props: { month: number; year: number }) {
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
})
