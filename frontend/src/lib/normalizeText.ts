/**
 * Normalização canônica de texto para matching — espelha o
 * IngredientNameNormalizer do backend: trim, remoção de acentos (NFD),
 * lowercase e colapso de whitespace interno para um único espaço.
 */
export function normalizeText(value: string | null | undefined): string {
  if (!value) return ''
  const trimmed = value.trim()
  if (!trimmed) return ''
  return trimmed
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
}
