import { describe, it, expect } from 'vitest'
import { catalogOriginLabel, catalogOriginPillColor } from '@/types/Category'

describe('catalogOrigin helpers', () => {
  it('mapeia cada origem para o rótulo pt-BR correto', () => {
    expect(catalogOriginLabel('MENUBANK')).toBe('MenuBank')
    expect(catalogOriginLabel('ANOTA_AI')).toBe('Anota.AI')
    expect(catalogOriginLabel('IFOOD')).toBe('iFood')
  })

  it('mapeia cada origem para a cor de pill correta', () => {
    expect(catalogOriginPillColor('MENUBANK')).toBe('gray')
    expect(catalogOriginPillColor('ANOTA_AI')).toBe('blue')
    expect(catalogOriginPillColor('IFOOD')).toBe('rose')
  })

  it('trata origem ausente (API antiga) como MenuBank', () => {
    expect(catalogOriginLabel(undefined)).toBe('MenuBank')
    expect(catalogOriginLabel(null)).toBe('MenuBank')
    expect(catalogOriginPillColor(undefined)).toBe('gray')
    expect(catalogOriginPillColor(null)).toBe('gray')
  })
})
