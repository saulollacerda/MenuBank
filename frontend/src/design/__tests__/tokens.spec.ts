import { describe, it, expect } from 'vitest'
import { brl } from '@/design/tokens'

describe('brl()', () => {
  it('formats normal values with 2 decimal places', () => {
    expect(brl(20)).toBe('R$ 20,00')
    expect(brl(120.5)).toBe('R$ 120,50')
  })

  it('does not display R$ 0,00 for non-zero values smaller than 0.01', () => {
    const result = brl(0.0001)
    expect(result).not.toBe('R$ 0,00')
  })

  it('shows up to 4 decimal places for small values', () => {
    expect(brl(0.0001)).toBe('R$ 0,0001')
    expect(brl(0.001)).toBe('R$ 0,001')
    expect(brl(0.005)).toBe('R$ 0,005')
  })

  it('does not add unnecessary trailing decimals for normal values', () => {
    expect(brl(20)).toBe('R$ 20,00')
    expect(brl(0.5)).toBe('R$ 0,50')
  })

  it('respects explicit decimals option', () => {
    expect(brl(20, { decimals: 0 })).toBe('R$ 20')
  })
})
