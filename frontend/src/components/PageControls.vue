<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue: string
  page: number
  totalPages: number
  totalElements: number
  placeholder?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'search', value: string): void
  (e: 'page-change', page: number): void
}>()

const localSearch = ref(props.modelValue)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

watch(
  () => props.modelValue,
  (v) => {
    if (v !== localSearch.value) localSearch.value = v
  },
)

function onInput(value: string) {
  localSearch.value = value
  emit('update:modelValue', value)
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => emit('search', value), 300)
}

function goToPage(p: number) {
  if (p < 0 || p >= props.totalPages || p === props.page) return
  emit('page-change', p)
}

function visiblePages(): number[] {
  const total = props.totalPages
  const current = props.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const pages: number[] = []
  pages.push(0)
  const start = Math.max(1, current - 1)
  const end = Math.min(total - 2, current + 1)
  if (start > 1) pages.push(-1) // ellipsis
  for (let i = start; i <= end; i++) pages.push(i)
  if (end < total - 2) pages.push(-1)
  pages.push(total - 1)
  return pages
}
</script>

<template>
  <div class="page-controls">
    <div class="search-row">
      <input
        type="search"
        class="search-input"
        :placeholder="placeholder ?? 'Buscar por nome...'"
        :value="localSearch"
        :disabled="loading"
        @input="onInput(($event.target as HTMLInputElement).value)"
      />
      <span v-if="totalElements > 0" class="total-label">
        {{ totalElements }} {{ totalElements === 1 ? 'resultado' : 'resultados' }}
      </span>
    </div>

    <div v-if="totalPages > 1" class="pagination">
      <button
        type="button"
        class="page-btn"
        :disabled="page === 0 || loading"
        @click="goToPage(page - 1)"
      >
        Anterior
      </button>

      <template v-for="(p, i) in visiblePages()" :key="i">
        <span v-if="p === -1" class="ellipsis">…</span>
        <button
          v-else
          type="button"
          class="page-btn"
          :class="{ active: p === page }"
          :disabled="loading"
          @click="goToPage(p)"
        >
          {{ p + 1 }}
        </button>
      </template>

      <button
        type="button"
        class="page-btn"
        :disabled="page >= totalPages - 1 || loading"
        @click="goToPage(page + 1)"
      >
        Próxima
      </button>
    </div>
  </div>
</template>

<style scoped>
.page-controls {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.search-input {
  flex: 1;
  max-width: 400px;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 0.95rem;
}

.search-input:focus {
  outline: 2px solid #3b82f6;
  outline-offset: -1px;
}

.total-label {
  color: #6b7280;
  font-size: 0.875rem;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 2.25rem;
  padding: 0.4rem 0.75rem;
  border: 1px solid #d1d5db;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
}

.page-btn:hover:not(:disabled) {
  background: #f3f4f6;
}

.page-btn.active {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ellipsis {
  padding: 0 0.25rem;
  color: #6b7280;
}
</style>
