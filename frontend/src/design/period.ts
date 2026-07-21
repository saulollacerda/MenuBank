export const MONTHS_PT = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
]

export const WEEKDAYS_PT_SHORT = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

export interface YearMonth {
  year: number
  month: number
}

export interface CalendarDay {
  /** ISO date (yyyy-mm-dd) */
  iso: string
  /** Day of the month (1-31) */
  day: number
  /** False for the leading/trailing filler days of the grid */
  inMonth: boolean
}

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

export function periodLabel(month: number, year: number): string {
  return `${MONTHS_PT[month - 1]} ${year}`
}

/** Formats a local Date as yyyy-mm-dd without going through UTC (avoids off-by-one days). */
export function toISODate(date: Date): string {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`
}

/** Parses a yyyy-mm-dd string as a local date at midnight. */
export function parseISODate(iso: string): Date {
  const [year = 0, month = 1, day = 1] = iso.split('-').map(Number)
  return new Date(year, month - 1, day)
}

/** Formats a yyyy-mm-dd string as dd/mm/aaaa. */
export function formatDateBR(iso: string): string {
  if (!iso) return ''
  const [year, month, day] = iso.split('-')
  return `${day}/${month}/${year}`
}

/** Human label for a date range, in pt-BR. */
export function rangeLabel(startIso: string, endIso: string): string {
  if (!startIso && !endIso) return ''
  if (!endIso || startIso === endIso) return formatDateBR(startIso)
  if (!startIso) return formatDateBR(endIso)
  return `${formatDateBR(startIso)} a ${formatDateBR(endIso)}`
}

export function addMonths(current: YearMonth, delta: number): YearMonth {
  const index = current.year * 12 + (current.month - 1) + delta
  return { year: Math.floor(index / 12), month: (index % 12) + 1 }
}

export function addDays(iso: string, delta: number): string {
  const date = parseISODate(iso)
  date.setDate(date.getDate() + delta)
  return toISODate(date)
}

/**
 * Builds the calendar grid for a month: whole weeks starting on Sunday, padded
 * with the neighbouring months' days so every row has 7 cells.
 */
export function buildCalendarDays(year: number, month: number): CalendarDay[] {
  const first = new Date(year, month - 1, 1)
  const cursor = new Date(year, month - 1, 1 - first.getDay())

  const days: CalendarDay[] = []
  // Always render whole weeks until the month is fully covered.
  do {
    for (let i = 0; i < 7; i++) {
      days.push({
        iso: toISODate(cursor),
        day: cursor.getDate(),
        inMonth: cursor.getMonth() === month - 1 && cursor.getFullYear() === year,
      })
      cursor.setDate(cursor.getDate() + 1)
    }
  } while (cursor.getMonth() === month - 1 && cursor.getFullYear() === year)

  return days
}
