import api from './api'

export type IfoodCatalogItemOutcome = 'IMPORTED' | 'LINKED' | 'SKIPPED'

export interface IfoodCatalogImportItem {
  name: string
  externalCode: string | null
  outcome: IfoodCatalogItemOutcome
  reason: string | null
}

export interface IfoodCatalogImportResult {
  importedProducts: number
  linkedProducts: number
  skippedProducts: number
  importedCategories: number
  linkedCategories: number
  items: IfoodCatalogImportItem[]
}

/** Maps a /catalog/import failure to user-facing pt-BR copy. */
export function importErrorMessage(err: unknown): string {
  const status = (err as { response?: { status?: number } })?.response?.status
  if (status === 409) {
    return 'Conecte sua conta do iFood antes de importar o cardápio.'
  }
  return 'Não foi possível importar o cardápio do iFood. Tente novamente em instantes.'
}

export const ifoodCatalogService = {
  async importCatalog(): Promise<IfoodCatalogImportResult> {
    const { data } = await api.post<IfoodCatalogImportResult>('/integrations/ifood/catalog/import')
    return data
  },
}
