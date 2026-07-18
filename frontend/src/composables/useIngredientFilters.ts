import { computed, type ComputedRef, type Ref } from 'vue'
import type { IngredientResponse, IngredientStatus } from '@/types/Ingredient'

/**
 * Sort options offered by the ingredients list. Empty string keeps the
 * backend order (as returned by the current page).
 */
export type IngredientSortKey = '' | 'name-asc' | 'name-desc' | 'cost-asc' | 'cost-desc'

export interface IngredientFilterState {
  /** Free-text name query (substring, accent-sensitive lowercase match). */
  nameQuery: Ref<string>
  /** Exact unit match, empty string means "all units". */
  unit: Ref<string>
  /** Status match, empty string means "all status". */
  status: Ref<'' | IngredientStatus>
  /** Minimum cost per unit (inclusive), null disables the lower bound. */
  minCost: Ref<number | null>
  /** Maximum cost per unit (inclusive), null disables the upper bound. */
  maxCost: Ref<number | null>
  /** Earliest creation day (inclusive) as YYYY-MM-DD, empty disables the bound. */
  createdFrom: Ref<string>
  /** Latest creation day (inclusive) as YYYY-MM-DD, empty disables the bound. */
  createdTo: Ref<string>
  sortBy: Ref<IngredientSortKey>
}

/**
 * Client-side filtering and sorting for the ingredients list. Operates on the
 * items already loaded in the store (same approach as the pre-existing unit and
 * status filters), so no extra backend calls are triggered.
 */
export function useIngredientFilters(
  items: Ref<IngredientResponse[]> | ComputedRef<IngredientResponse[]>,
  state: IngredientFilterState,
) {
  const filtered = computed<IngredientResponse[]>(() => {
    const query = state.nameQuery.value.trim().toLowerCase()
    const min = state.minCost.value
    const max = state.maxCost.value
    const unit = state.unit.value
    const status = state.status.value
    const from = state.createdFrom.value
    const to = state.createdTo.value
    const hasDateFilter = Boolean(from || to)

    return items.value.filter((item) => {
      if (query && !item.name.toLowerCase().includes(query)) return false
      if (unit && item.unit !== unit) return false
      if (status && item.status !== status) return false
      const cost = Number(item.costPerUnit)
      if (min != null && cost < min) return false
      if (max != null && cost > max) return false
      if (hasDateFilter) {
        // Legacy rows have unknown creation dates: drop them once a bound is active.
        if (!item.createdAt) return false
        // ISO dates compare correctly as strings; use only the date part so
        // both bounds are inclusive of the whole boundary day.
        const day = item.createdAt.slice(0, 10)
        if (from && day < from) return false
        if (to && day > to) return false
      }
      return true
    })
  })

  const sorted = computed<IngredientResponse[]>(() => {
    const list = [...filtered.value]
    switch (state.sortBy.value) {
      case 'name-asc':
        return list.sort((a, b) =>
          a.name.localeCompare(b.name, 'pt-BR', { sensitivity: 'base' }),
        )
      case 'name-desc':
        return list.sort((a, b) =>
          b.name.localeCompare(a.name, 'pt-BR', { sensitivity: 'base' }),
        )
      case 'cost-asc':
        return list.sort((a, b) => Number(a.costPerUnit) - Number(b.costPerUnit))
      case 'cost-desc':
        return list.sort((a, b) => Number(b.costPerUnit) - Number(a.costPerUnit))
      default:
        return list
    }
  })

  /** Count of value filters currently narrowing the list (name/sort excluded). */
  const activeFilterCount = computed(() => {
    let count = 0
    if (state.unit.value) count += 1
    if (state.status.value) count += 1
    if (state.minCost.value != null) count += 1
    if (state.maxCost.value != null) count += 1
    if (state.createdFrom.value) count += 1
    if (state.createdTo.value) count += 1
    return count
  })

  function reset() {
    state.unit.value = ''
    state.status.value = ''
    state.minCost.value = null
    state.maxCost.value = null
    state.createdFrom.value = ''
    state.createdTo.value = ''
    state.sortBy.value = ''
  }

  return { filtered, sorted, activeFilterCount, reset }
}
