<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'
import { MONTHS_PT, periodLabel } from './period'

const props = defineProps<{
  month: number
  year: number
}>()

const emit = defineEmits<{
  (e: 'update:month', value: number): void
  (e: 'update:year', value: number): void
}>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)

const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

const label = computed(() => periodLabel(props.month, props.year))

const yearOptions = computed(() => {
  const start = 2026
  const end = Math.max(currentYear, start)
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})

const monthOptions = computed(() => {
  const maxMonth = props.year === currentYear ? currentMonth : 12
  return MONTHS_PT.slice(0, maxMonth).map((name, i) => ({ value: i + 1, label: name }))
})

watch(
  () => props.year,
  () => {
    const max = props.year === currentYear ? currentMonth : 12
    if (props.month > max) emit('update:month', max)
  },
)

function selectMonth(value: number) {
  emit('update:month', value)
  open.value = false
}

function selectYear(value: number) {
  emit('update:year', value)
}

function onDocumentClick(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) open.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))
</script>

<template>
  <div ref="root" style="position: relative" @keydown="onKeydown">
    <button
      type="button"
      data-testid="period-picker-toggle"
      class="ui-period-toggle"
      :aria-expanded="open"
      title="Selecionar período"
      :style="{
        background: UI.bg,
        border: `1px solid ${UI.border}`,
        color: UI.text,
      }"
      @click="open = !open"
    >
      <UIIcon name="clock" :size="14" />
      {{ label }}
      <UIIcon name="chevDown" :size="11" />
    </button>

    <div
      v-if="open"
      data-testid="period-picker-menu"
      :style="{
        position: 'absolute',
        top: 'calc(100% + 6px)',
        right: 0,
        zIndex: 20,
        width: '264px',
        background: UI.panel,
        border: `1px solid ${UI.border}`,
        borderRadius: '11px',
        boxShadow: '0 8px 24px rgba(15,23,42,0.12)',
        padding: '12px',
      }"
    >
      <div
        :style="{
          fontSize: '11px',
          fontWeight: 600,
          color: UI.textSub,
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          marginBottom: '8px',
        }"
      >
        Ano
      </div>
      <div style="display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px">
        <button
          v-for="y in yearOptions"
          :key="y"
          type="button"
          :data-testid="`period-year-${y}`"
          class="ui-period-option"
          :style="{
            background: y === year ? UI.text : UI.bg,
            color: y === year ? '#fff' : UI.text,
            border: `1px solid ${y === year ? UI.text : UI.border}`,
          }"
          @click="selectYear(y)"
        >
          {{ y }}
        </button>
      </div>
      <div
        :style="{
          fontSize: '11px',
          fontWeight: 600,
          color: UI.textSub,
          textTransform: 'uppercase',
          letterSpacing: '0.5px',
          marginBottom: '8px',
        }"
      >
        Mês
      </div>
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px">
        <button
          v-for="m in monthOptions"
          :key="m.value"
          type="button"
          :data-testid="`period-month-${m.value}`"
          class="ui-period-option"
          :style="{
            background: m.value === month ? UI.text : UI.bg,
            color: m.value === month ? '#fff' : UI.text,
            border: `1px solid ${m.value === month ? UI.text : UI.border}`,
          }"
          @click="selectMonth(m.value)"
        >
          {{ m.label.slice(0, 3) }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ui-period-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 9px;
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color 0.12s ease;
}

.ui-period-toggle:hover {
  border-color: #94a3b8;
}

.ui-period-option {
  padding: 6px 8px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  text-align: center;
}
</style>
