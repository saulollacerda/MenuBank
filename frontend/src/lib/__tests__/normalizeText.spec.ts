import { describe, it, expect } from 'vitest'
import { normalizeText } from '@/lib/normalizeText'

describe('normalizeText', () => {
  it('lowercases and trims', () => {
    expect(normalizeText('  Maria Clara  ')).toBe('maria clara')
  })

  it('strips accents/diacritics', () => {
    expect(normalizeText('João Açaí')).toBe('joao acai')
    expect(normalizeText('CRÈME DE CHOCOLATE')).toBe('creme de chocolate')
  })

  it('collapses internal whitespace', () => {
    expect(normalizeText('Creme  de   Chocolate')).toBe('creme de chocolate')
  })

  it('returns empty string for null/undefined/blank', () => {
    expect(normalizeText(null)).toBe('')
    expect(normalizeText(undefined)).toBe('')
    expect(normalizeText('   ')).toBe('')
  })
})
