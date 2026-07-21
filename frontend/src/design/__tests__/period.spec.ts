import { describe, it, expect } from 'vitest'

import {
  MONTHS_PT,
  WEEKDAYS_PT_SHORT,
  periodLabel,
  toISODate,
  parseISODate,
  formatDateBR,
  rangeLabel,
  buildCalendarDays,
  addMonths,
} from '@/design/period'

describe('period helpers', () => {
  it('expõe os meses e os dias da semana em pt-BR', () => {
    expect(MONTHS_PT[10]).toBe('Novembro')
    expect(WEEKDAYS_PT_SHORT).toHaveLength(7)
    expect(WEEKDAYS_PT_SHORT[0]).toBe('Dom')
  })

  it('periodLabel formata mês e ano', () => {
    expect(periodLabel(11, 2026)).toBe('Novembro 2026')
  })

  it('toISODate converte uma data local para ISO sem deslocamento de fuso', () => {
    expect(toISODate(new Date(2026, 10, 16))).toBe('2026-11-16')
    expect(toISODate(new Date(2026, 0, 1))).toBe('2026-01-01')
  })

  it('parseISODate devolve uma data local no início do dia', () => {
    const d = parseISODate('2026-11-16')
    expect(d.getFullYear()).toBe(2026)
    expect(d.getMonth()).toBe(10)
    expect(d.getDate()).toBe(16)
  })

  it('formatDateBR formata a data no padrão dd/mm/aaaa', () => {
    expect(formatDateBR('2026-11-16')).toBe('16/11/2026')
    expect(formatDateBR('2026-12-07')).toBe('07/12/2026')
    expect(formatDateBR('')).toBe('')
  })

  it('rangeLabel descreve o intervalo em pt-BR', () => {
    expect(rangeLabel('2026-11-16', '2026-12-07')).toBe('16/11/2026 a 07/12/2026')
  })

  it('rangeLabel exibe uma única data quando início e fim coincidem', () => {
    expect(rangeLabel('2026-11-16', '2026-11-16')).toBe('16/11/2026')
  })

  it('addMonths avança e retrocede meses virando o ano', () => {
    expect(addMonths({ year: 2026, month: 12 }, 1)).toEqual({ year: 2027, month: 1 })
    expect(addMonths({ year: 2026, month: 1 }, -1)).toEqual({ year: 2025, month: 12 })
    expect(addMonths({ year: 2026, month: 5 }, 2)).toEqual({ year: 2026, month: 7 })
  })

  it('buildCalendarDays monta uma grade de semanas completas começando no domingo', () => {
    const days = buildCalendarDays(2026, 11)
    expect(days.length % 7).toBe(0)
    expect(days[0]!.iso <= '2026-11-01').toBe(true)
    const first = days.find((d) => d.iso === '2026-11-01')
    expect(first?.inMonth).toBe(true)
    const last = days.find((d) => d.iso === '2026-11-30')
    expect(last?.inMonth).toBe(true)
    // dias fora do mês corrente entram como preenchimento
    expect(days.some((d) => !d.inMonth)).toBe(true)
  })

  it('buildCalendarDays marca o número do dia', () => {
    const days = buildCalendarDays(2026, 11)
    expect(days.find((d) => d.iso === '2026-11-16')?.day).toBe(16)
  })
})
