/** Where a catalog record came from: created in MenuBank or imported. */
export type CatalogOrigin = 'MENUBANK' | 'ANOTA_AI' | 'IFOOD'

export function catalogOriginLabel(origin?: CatalogOrigin | null): string {
  if (origin === 'ANOTA_AI') return 'Anota.AI'
  if (origin === 'IFOOD') return 'iFood'
  return 'MenuBank'
}

export function catalogOriginPillColor(origin?: CatalogOrigin | null): 'gray' | 'blue' | 'rose' {
  if (origin === 'ANOTA_AI') return 'blue'
  if (origin === 'IFOOD') return 'rose'
  return 'gray'
}

export interface CategoryRequest {
  name: string
}

export interface CategoryResponse {
  id: string
  name: string
  origin?: CatalogOrigin
}
