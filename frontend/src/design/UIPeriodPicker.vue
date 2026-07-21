<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import UIIcon from './UIIcon.vue'
import {
  MONTHS_PT,
  WEEKDAYS_PT_SHORT,
  addDays,
  addMonths,
  buildCalendarDays,
  periodLabel,
  rangeLabel,
  toISODate,
} from './period'

type FilterMode = 'month' | 'custom'

export interface PeriodChange {
  mode: FilterMode
  startDate?: string
  endDate?: string
}

const props = withDefaults(
  defineProps<{
    month: number
    year: number
    mode?: FilterMode
    startDate?: string
    endDate?: string
  }>(),
  { mode: 'month', startDate: '', endDate: '' },
)

const emit = defineEmits<{
  (e: 'update:month', value: number): void
  (e: 'update:year', value: number): void
  (e: 'update:mode', value: FilterMode): void
  (e: 'update:startDate', value: string): void
  (e: 'update:endDate', value: string): void
  (e: 'change', value: PeriodChange): void
}>()

const open = ref(false)
const root = ref<HTMLElement | null>(null)
const tab = ref<FilterMode>(props.mode)

const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1
const today = toISODate(new Date())

const label = computed(() => {
  if (props.mode === 'custom' && (props.startDate || props.endDate)) {
    return rangeLabel(props.startDate, props.endDate)
  }
  return periodLabel(props.month, props.year)
})

/* ---------------------------------------------------------------- month tab */

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
  emit('update:mode', 'month')
  emit('change', { mode: 'month' })
  open.value = false
}

function selectYear(value: number) {
  emit('update:year', value)
  emit('update:mode', 'month')
  emit('change', { mode: 'month' })
}

/* --------------------------------------------------------------- custom tab */

const viewMonth = ref({ year: props.year, month: props.month })
const pendingStart = ref<string | null>(null)

function resetCalendarView() {
  const anchor = props.mode === 'custom' && props.startDate
    ? { year: Number(props.startDate.slice(0, 4)), month: Number(props.startDate.slice(5, 7)) }
    : { year: props.year, month: props.month }
  // Never open on a month past the current one.
  if (anchor.year > currentYear || (anchor.year === currentYear && anchor.month > currentMonth)) {
    viewMonth.value = { year: currentYear, month: currentMonth }
  } else {
    viewMonth.value = anchor
  }
  pendingStart.value = null
}

const calendarTitle = computed(() => periodLabel(viewMonth.value.month, viewMonth.value.year))

const calendarDays = computed(() => buildCalendarDays(viewMonth.value.year, viewMonth.value.month))

const canGoNext = computed(() => {
  const { year, month } = viewMonth.value
  return year < currentYear || (year === currentYear && month < currentMonth)
})

function shiftCalendar(delta: number) {
  const next = addMonths(viewMonth.value, delta)
  if (delta > 0 && !canGoNext.value) return
  viewMonth.value = next
}

const selectedStart = computed(() =>
  pendingStart.value ?? (props.mode === 'custom' ? props.startDate : ''),
)
const selectedEnd = computed(() =>
  pendingStart.value ? '' : props.mode === 'custom' ? props.endDate : '',
)

function isRangeEdge(iso: string): boolean {
  return iso === selectedStart.value || iso === selectedEnd.value
}

function isInRange(iso: string): boolean {
  const start = selectedStart.value
  const end = selectedEnd.value
  if (!start || !end) return false
  return iso > start && iso < end
}

function isFuture(iso: string): boolean {
  return iso > today
}

const rangeHint = computed(() => {
  if (pendingStart.value) return `Início ${formatBR(pendingStart.value)} · escolha a data final`
  if (props.mode === 'custom' && props.startDate) {
    return rangeLabel(props.startDate, props.endDate)
  }
  return 'Escolha a data inicial e a data final'
})

function formatBR(iso: string): string {
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

function applyRange(startDate: string, endDate: string) {
  emit('update:mode', 'custom')
  emit('update:startDate', startDate)
  emit('update:endDate', endDate)
  emit('change', { mode: 'custom', startDate, endDate })
  pendingStart.value = null
  open.value = false
}

function selectDay(iso: string) {
  if (isFuture(iso)) return
  if (!pendingStart.value) {
    pendingStart.value = iso
    return
  }
  // An earlier second click restarts the selection instead of producing an
  // invalid range (end before start is never emitted).
  if (iso < pendingStart.value) {
    pendingStart.value = iso
    return
  }
  applyRange(pendingStart.value, iso)
}

const presets = computed(() => [
  { id: 'today', label: 'Hoje', start: today, end: today },
  { id: '7d', label: 'Últimos 7 dias', start: addDays(today, -6), end: today },
  { id: '30d', label: 'Últimos 30 dias', start: addDays(today, -29), end: today },
  { id: 'month', label: 'Este mês', start: `${today.slice(0, 7)}-01`, end: today },
])

/* -------------------------------------------------------------------- shell */

function toggleOpen() {
  open.value = !open.value
  if (open.value) {
    tab.value = props.mode
    resetCalendarView()
  }
}

function selectTab(value: FilterMode) {
  tab.value = value
  if (value === 'custom') resetCalendarView()
}

function onDocumentClick(event: MouseEvent) {
  if (root.value && !root.value.contains(event.target as Node)) open.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') open.value = false
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))

/* Shared class string — Tailwind preflight is disabled app-wide, so buttons
   need the user-agent chrome reset explicitly. It deliberately carries no
   border, background or padding: those would collide with the per-button
   utilities below, and conflicting utilities in the same cascade layer are
   resolved by stylesheet order rather than by the order written here. Each
   button therefore declares its own border and background. */
const RESET = 'm-0 cursor-pointer appearance-none font-[inherit]'
</script>

<template>
  <div ref="root" class="relative" @keydown="onKeydown">
    <button
      type="button"
      data-testid="period-picker-toggle"
      :aria-expanded="open"
      title="Selecionar período"
      :class="[
        RESET,
        'flex items-center gap-2 rounded-[9px] border border-solid border-slate-200 bg-white',
        'px-3.5 py-2 text-[12.5px] font-medium text-slate-900 transition-colors',
        'hover:border-slate-400',
      ]"
      @click="toggleOpen"
    >
      <UIIcon name="clock" :size="14" />
      {{ label }}
      <UIIcon name="chevDown" :size="11" />
    </button>

    <div
      v-if="open"
      data-testid="period-picker-menu"
      class="absolute right-0 top-[calc(100%+6px)] z-20 w-[320px] rounded-xl border border-solid border-slate-200 bg-white p-3 text-slate-900 shadow-[0_12px_32px_rgba(15,23,42,0.14)]"
    >
      <!-- Tabs -->
      <div class="mb-3 flex gap-1 rounded-lg bg-slate-100 p-1">
        <button
          type="button"
          data-testid="period-tab-month"
          :class="[
            RESET,
            'flex-1 rounded-md border-0 py-1.5 text-center text-xs font-semibold transition-colors',
            tab === 'month' ? 'bg-white text-slate-900 shadow-sm' : 'bg-transparent text-slate-500 hover:text-slate-700',
          ]"
          @click="selectTab('month')"
        >
          Mês
        </button>
        <button
          type="button"
          data-testid="period-tab-custom"
          :class="[
            RESET,
            'flex-1 rounded-md border-0 py-1.5 text-center text-xs font-semibold transition-colors',
            tab === 'custom' ? 'bg-white text-slate-900 shadow-sm' : 'bg-transparent text-slate-500 hover:text-slate-700',
          ]"
          @click="selectTab('custom')"
        >
          Período
        </button>
      </div>

      <!-- Month tab -->
      <div v-if="tab === 'month'">
        <div class="mb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
          Ano
        </div>
        <div class="mb-3 flex flex-wrap gap-1.5">
          <button
            v-for="y in yearOptions"
            :key="y"
            type="button"
            :data-testid="`period-year-${y}`"
            :class="[
              RESET,
              'rounded-md border border-solid px-2 py-1.5 text-xs font-medium transition-colors',
              y === year
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-white text-slate-700 hover:border-slate-400',
            ]"
            @click="selectYear(y)"
          >
            {{ y }}
          </button>
        </div>
        <div class="mb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
          Mês
        </div>
        <div class="grid grid-cols-[repeat(3,minmax(0,1fr))] gap-1.5">
          <button
            v-for="m in monthOptions"
            :key="m.value"
            type="button"
            :data-testid="`period-month-${m.value}`"
            :class="[
              RESET,
              'rounded-md border border-solid px-2 py-1.5 text-center text-xs font-medium transition-colors',
              m.value === month
                ? 'border-slate-900 bg-slate-900 text-white'
                : 'border-slate-200 bg-white text-slate-700 hover:border-slate-400',
            ]"
            @click="selectMonth(m.value)"
          >
            {{ m.label.slice(0, 3) }}
          </button>
        </div>
      </div>

      <!-- Custom range tab -->
      <div v-else data-testid="period-calendar">
        <div class="mb-3 grid grid-cols-[repeat(2,minmax(0,1fr))] gap-1.5">
          <button
            v-for="p in presets"
            :key="p.id"
            type="button"
            :data-testid="`period-preset-${p.id}`"
            :class="[
              RESET,
              'rounded-md border border-solid border-slate-200 bg-white px-2 py-1.5 text-[11.5px] font-medium text-slate-700 transition-colors hover:border-slate-400 hover:bg-slate-50',
            ]"
            @click="applyRange(p.start, p.end)"
          >
            {{ p.label }}
          </button>
        </div>

        <div class="mb-2 flex items-center justify-between">
          <button
            type="button"
            data-testid="period-calendar-prev"
            title="Mês anterior"
            :class="[
              RESET,
              'flex h-7 w-7 items-center justify-center rounded-md border-0 bg-transparent text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900',
            ]"
            @click="shiftCalendar(-1)"
          >
            <UIIcon name="chevDown" :size="12" class="rotate-90" />
          </button>
          <div data-testid="period-calendar-title" class="text-[12.5px] font-semibold text-slate-900">
            {{ calendarTitle }}
          </div>
          <button
            type="button"
            data-testid="period-calendar-next"
            title="Próximo mês"
            :disabled="!canGoNext"
            :class="[
              RESET,
              'flex h-7 w-7 items-center justify-center rounded-md border-0 bg-transparent transition-colors',
              canGoNext
                ? 'text-slate-500 hover:bg-slate-100 hover:text-slate-900'
                : 'cursor-not-allowed text-slate-300',
            ]"
            @click="shiftCalendar(1)"
          >
            <UIIcon name="chevDown" :size="12" class="-rotate-90" />
          </button>
        </div>

        <div class="grid grid-cols-[repeat(7,minmax(0,1fr))] gap-y-1 text-center">
          <div
            v-for="w in WEEKDAYS_PT_SHORT"
            :key="w"
            class="py-1 text-[10px] font-semibold uppercase tracking-wide text-slate-400"
          >
            {{ w.slice(0, 1) }}
          </div>
          <button
            v-for="d in calendarDays"
            :key="d.iso"
            type="button"
            :data-testid="`period-day-${d.iso}`"
            :disabled="isFuture(d.iso)"
            :class="[
              RESET,
              'mx-auto flex h-8 w-8 items-center justify-center rounded-lg border-0 text-xs font-medium tabular-nums',
              isFuture(d.iso)
                ? 'cursor-not-allowed bg-transparent text-slate-300'
                : isRangeEdge(d.iso)
                  ? 'bg-slate-900 text-white'
                  : isInRange(d.iso)
                    ? 'bg-slate-900/10 text-slate-900'
                    : d.inMonth
                      ? 'bg-transparent text-slate-700 hover:bg-slate-100'
                      : 'bg-transparent text-slate-400 hover:bg-slate-100',
            ]"
            @click="selectDay(d.iso)"
          >
            {{ d.day }}
          </button>
        </div>

        <div
          data-testid="period-range-hint"
          class="mt-3 rounded-lg bg-slate-50 px-2.5 py-2 text-center text-[11.5px] font-medium text-slate-600"
        >
          {{ rangeHint }}
        </div>
      </div>
    </div>
  </div>
</template>
