import { describe, it, expect } from 'vitest'
import { ref } from 'vue'
import {
  useIngredientFilters,
  type IngredientFilterState,
} from '@/composables/useIngredientFilters'
import type { IngredientResponse } from '@/types/Ingredient'

function makeIngredient(over: Partial<IngredientResponse>): IngredientResponse {
  return {
    id: over.id ?? crypto.randomUUID(),
    name: over.name ?? 'Ingrediente',
    unit: over.unit ?? 'g',
    costPerUnit: over.costPerUnit ?? 1,
    salePrice: over.salePrice ?? null,
    defaultQuantity: over.defaultQuantity ?? 0,
    status: over.status ?? 'ACTIVE',
    createdAt: 'createdAt' in over ? over.createdAt : '2026-01-01T12:00:00',
  }
}

function makeState(over: Partial<Record<keyof IngredientFilterState, unknown>> = {}) {
  const state: IngredientFilterState = {
    nameQuery: ref((over.nameQuery as string) ?? ''),
    unit: ref((over.unit as string) ?? ''),
    status: ref((over.status as '' | 'ACTIVE' | 'INACTIVE') ?? ''),
    minCost: ref((over.minCost as number | null) ?? null),
    maxCost: ref((over.maxCost as number | null) ?? null),
    createdFrom: ref((over.createdFrom as string) ?? ''),
    createdTo: ref((over.createdTo as string) ?? ''),
    sortBy: ref((over.sortBy as IngredientFilterState['sortBy']['value']) ?? ''),
  }
  return state
}

const sample: IngredientResponse[] = [
  makeIngredient({ id: '1', name: 'Açúcar', unit: 'g', costPerUnit: 0.02, status: 'ACTIVE' }),
  makeIngredient({ id: '2', name: 'Banana', unit: 'kg', costPerUnit: 5, status: 'INACTIVE' }),
  makeIngredient({ id: '3', name: 'Chocolate', unit: 'g', costPerUnit: 10, status: 'ACTIVE' }),
  makeIngredient({ id: '4', name: 'Doce de leite', unit: 'kg', costPerUnit: 25, status: 'ACTIVE' }),
]

describe('useIngredientFilters', () => {
  it('should return all items when no filter is set', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState())
    expect(sorted.value.map((i) => i.id)).toEqual(['1', '2', '3', '4'])
  })

  it('should filter by minimum cost only (inclusive of the boundary)', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ minCost: 5 }))
    expect(sorted.value.map((i) => i.id)).toEqual(['2', '3', '4'])
  })

  it('should filter by maximum cost only (inclusive of the boundary)', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ maxCost: 10 }))
    expect(sorted.value.map((i) => i.id)).toEqual(['1', '2', '3'])
  })

  it('should filter by both minimum and maximum cost', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ minCost: 5, maxCost: 10 }))
    expect(sorted.value.map((i) => i.id)).toEqual(['2', '3'])
  })

  it('should combine name, cost and status filters', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(
      items,
      makeState({ nameQuery: 'a', minCost: 1, status: 'ACTIVE' }),
    )
    // name contains "a": Açúcar(0.02), Banana(5), Chocolate(10)
    // minCost 1 removes Açúcar; status ACTIVE removes Banana
    expect(sorted.value.map((i) => i.id)).toEqual(['3'])
  })

  it('should filter by unit', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ unit: 'kg' }))
    expect(sorted.value.map((i) => i.id)).toEqual(['2', '4'])
  })

  it('should sort by name ascending (accent-insensitive)', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ sortBy: 'name-asc' }))
    expect(sorted.value.map((i) => i.name)).toEqual([
      'Açúcar',
      'Banana',
      'Chocolate',
      'Doce de leite',
    ])
  })

  it('should sort by name descending', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ sortBy: 'name-desc' }))
    expect(sorted.value.map((i) => i.name)).toEqual([
      'Doce de leite',
      'Chocolate',
      'Banana',
      'Açúcar',
    ])
  })

  it('should sort by cost ascending', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ sortBy: 'cost-asc' }))
    expect(sorted.value.map((i) => i.costPerUnit)).toEqual([0.02, 5, 10, 25])
  })

  it('should sort by cost descending', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ sortBy: 'cost-desc' }))
    expect(sorted.value.map((i) => i.costPerUnit)).toEqual([25, 10, 5, 0.02])
  })

  it('should not mutate the source array when sorting', () => {
    const items = ref(sample)
    const { sorted } = useIngredientFilters(items, makeState({ sortBy: 'cost-desc' }))
    // touch computed
    expect(sorted.value.length).toBe(4)
    expect(items.value.map((i) => i.id)).toEqual(['1', '2', '3', '4'])
  })

  it('should report the number of active filters (name/sort excluded)', () => {
    const state = makeState({
      unit: 'kg',
      minCost: 5,
      createdFrom: '2026-01-01',
      sortBy: 'cost-asc',
    })
    const { activeFilterCount } = useIngredientFilters(ref(sample), state)
    expect(activeFilterCount.value).toBe(3)
  })

  it('should reset unit, status, cost, date and sort filters', () => {
    const state = makeState({
      unit: 'kg',
      status: 'ACTIVE',
      minCost: 5,
      maxCost: 10,
      createdFrom: '2026-01-01',
      createdTo: '2026-02-01',
      sortBy: 'cost-asc',
    })
    const { reset, activeFilterCount } = useIngredientFilters(ref(sample), state)
    reset()
    expect(state.unit.value).toBe('')
    expect(state.status.value).toBe('')
    expect(state.minCost.value).toBeNull()
    expect(state.maxCost.value).toBeNull()
    expect(state.createdFrom.value).toBe('')
    expect(state.createdTo.value).toBe('')
    expect(state.sortBy.value).toBe('')
    expect(activeFilterCount.value).toBe(0)
  })

  it('should react to filter state changes', () => {
    const state = makeState()
    const { sorted } = useIngredientFilters(ref(sample), state)
    expect(sorted.value.length).toBe(4)
    state.minCost.value = 10
    expect(sorted.value.map((i) => i.id)).toEqual(['3', '4'])
  })

  describe('creation-date filtering', () => {
    const dated: IngredientResponse[] = [
      makeIngredient({ id: 'a', name: 'A', createdAt: '2026-01-05T08:00:00' }),
      makeIngredient({ id: 'b', name: 'B', createdAt: '2026-01-10T23:30:00' }),
      makeIngredient({ id: 'c', name: 'C', createdAt: '2026-01-20T00:00:00' }),
      makeIngredient({ id: 'legacy', name: 'Legacy', createdAt: null }),
    ]

    it('should include all rows (legacy null included) when no date filter is set', () => {
      const { sorted } = useIngredientFilters(ref(dated), makeState())
      expect(sorted.value.map((i) => i.id)).toEqual(['a', 'b', 'c', 'legacy'])
    })

    it('should filter by createdFrom inclusive of the boundary day', () => {
      const { sorted } = useIngredientFilters(ref(dated), makeState({ createdFrom: '2026-01-10' }))
      // 2026-01-10 rows are kept; legacy null excluded once a date filter is active
      expect(sorted.value.map((i) => i.id)).toEqual(['b', 'c'])
    })

    it('should filter by createdTo inclusive of the boundary day', () => {
      const { sorted } = useIngredientFilters(ref(dated), makeState({ createdTo: '2026-01-10' }))
      // includes the whole 2026-01-10 day even at 23:30
      expect(sorted.value.map((i) => i.id)).toEqual(['a', 'b'])
    })

    it('should filter by a closed date range', () => {
      const { sorted } = useIngredientFilters(
        ref(dated),
        makeState({ createdFrom: '2026-01-05', createdTo: '2026-01-10' }),
      )
      expect(sorted.value.map((i) => i.id)).toEqual(['a', 'b'])
    })

    it('should exclude legacy null-createdAt rows when any date bound is active', () => {
      const fromOnly = useIngredientFilters(ref(dated), makeState({ createdFrom: '2026-01-01' }))
      expect(fromOnly.sorted.value.map((i) => i.id)).not.toContain('legacy')
      const toOnly = useIngredientFilters(ref(dated), makeState({ createdTo: '2026-12-31' }))
      expect(toOnly.sorted.value.map((i) => i.id)).not.toContain('legacy')
    })

    it('should combine name, cost and date filters', () => {
      const items: IngredientResponse[] = [
        makeIngredient({ id: '1', name: 'Ameixa', costPerUnit: 3, createdAt: '2026-01-06T10:00:00' }),
        makeIngredient({ id: '2', name: 'Amora', costPerUnit: 12, createdAt: '2026-01-06T10:00:00' }),
        makeIngredient({ id: '3', name: 'Amora', costPerUnit: 12, createdAt: '2026-02-01T10:00:00' }),
      ]
      const { sorted } = useIngredientFilters(
        ref(items),
        makeState({
          nameQuery: 'amora',
          minCost: 5,
          createdFrom: '2026-01-01',
          createdTo: '2026-01-31',
        }),
      )
      expect(sorted.value.map((i) => i.id)).toEqual(['2'])
    })
  })

  describe('creation-date sorting', () => {
    const dated: IngredientResponse[] = [
      makeIngredient({ id: 'old', name: 'Antiga', createdAt: '2026-01-05T08:00:00' }),
      makeIngredient({ id: 'mid', name: 'Meio', createdAt: '2026-01-10T23:30:00' }),
      makeIngredient({ id: 'new', name: 'Nova', createdAt: '2026-01-20T00:00:00' }),
      makeIngredient({ id: 'legacy', name: 'Legada', createdAt: null }),
    ]

    it('should sort by most recent first with null createdAt last', () => {
      const { sorted } = useIngredientFilters(ref(dated), makeState({ sortBy: 'created-desc' }))
      expect(sorted.value.map((i) => i.id)).toEqual(['new', 'mid', 'old', 'legacy'])
    })

    it('should sort by oldest first with null createdAt last', () => {
      const { sorted } = useIngredientFilters(ref(dated), makeState({ sortBy: 'created-asc' }))
      expect(sorted.value.map((i) => i.id)).toEqual(['old', 'mid', 'new', 'legacy'])
    })

    it('should keep null createdAt rows last in both directions (multiple legacy rows)', () => {
      const items: IngredientResponse[] = [
        makeIngredient({ id: 'a', createdAt: null }),
        makeIngredient({ id: 'b', createdAt: '2026-03-01T00:00:00' }),
        makeIngredient({ id: 'c', createdAt: null }),
        makeIngredient({ id: 'd', createdAt: '2026-01-01T00:00:00' }),
      ]
      const desc = useIngredientFilters(ref(items), makeState({ sortBy: 'created-desc' }))
      expect(desc.sorted.value.slice(0, 2).map((i) => i.id)).toEqual(['b', 'd'])
      expect(desc.sorted.value.slice(2).map((i) => i.id).sort()).toEqual(['a', 'c'])

      const asc = useIngredientFilters(ref(items), makeState({ sortBy: 'created-asc' }))
      expect(asc.sorted.value.slice(0, 2).map((i) => i.id)).toEqual(['d', 'b'])
      expect(asc.sorted.value.slice(2).map((i) => i.id).sort()).toEqual(['a', 'c'])
    })
  })
})
