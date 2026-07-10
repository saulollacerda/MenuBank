export const MONTHS_PT = [
  'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
  'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro',
]

export function periodLabel(month: number, year: number): string {
  return `${MONTHS_PT[month - 1]} ${year}`
}
