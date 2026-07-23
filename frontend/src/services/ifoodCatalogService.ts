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

export type IfoodCatalogPublishOutcome = 'PUBLISHED' | 'SKIPPED' | 'FAILED'

export interface IfoodCatalogPublishItem {
  productId: string
  name: string
  externalCode: string | null
  outcome: IfoodCatalogPublishOutcome
  reason: string | null
}

export interface IfoodCatalogPublishResult {
  publishedProducts: number
  skippedProducts: number
  items: IfoodCatalogPublishItem[]
}

export type IfoodCatalogItemStatus = 'AVAILABLE' | 'UNAVAILABLE'

export interface IfoodCatalogStatusItem {
  productId: string
  status: IfoodCatalogItemStatus
}

export interface IfoodCatalogBatchSkip {
  productId: string
  reason: string
}

/** Acceptance of an asynchronous catalog operation (price/status batch). */
export interface IfoodCatalogBatchAccepted {
  /** Null when nothing was sent to iFood — every requested product landed in `skipped`. */
  batchId: string | null
  requested: number
  skipped: IfoodCatalogBatchSkip[]
}

export interface IfoodCatalogBatchResult {
  resourceId: string
  result: string
}

export interface IfoodCatalogBatchStatus {
  batchId: string
  status: string
  successCount: number
  failureCount: number
  results: IfoodCatalogBatchResult[]
}

/** Maps a catalog write (publish/price/status/batch) failure to user-facing pt-BR copy. */
export function publishErrorMessage(err: unknown): string {
  const status = (err as { response?: { status?: number } })?.response?.status
  switch (status) {
    case 409:
      return 'Conecte sua conta do iFood — a autorização não está ativa ou expirou.'
    case 422:
      return 'Alguns dados são inválidos para o iFood. Revise nome (até 100 caracteres), descrição e preço dos produtos.'
    case 404:
      return 'Item não encontrado no iFood. Publique o produto antes de atualizar preço ou status.'
    case 503:
      return 'O iFood está indisponível no momento. Tente novamente em instantes.'
    default:
      return 'Não foi possível concluir a operação no iFood. Tente novamente em instantes.'
  }
}

export const ifoodCatalogService = {
  async importCatalog(): Promise<IfoodCatalogImportResult> {
    const { data } = await api.post<IfoodCatalogImportResult>('/integrations/ifood/catalog/import')
    return data
  },

  /** Publishes products to the WHITELABEL (Cardápio Digital) context. Empty body = all active products. */
  async publishCatalog(productIds?: string[]): Promise<IfoodCatalogPublishResult> {
    const body = productIds && productIds.length > 0 ? { productIds } : {}
    const { data } = await api.post<IfoodCatalogPublishResult>(
      '/integrations/ifood/catalog/publish',
      body,
    )
    return data
  },

  async syncPrices(productIds: string[]): Promise<IfoodCatalogBatchAccepted> {
    const { data } = await api.patch<IfoodCatalogBatchAccepted>(
      '/integrations/ifood/catalog/prices',
      { productIds },
    )
    return data
  },

  async syncStatus(items: IfoodCatalogStatusItem[]): Promise<IfoodCatalogBatchAccepted> {
    const { data } = await api.patch<IfoodCatalogBatchAccepted>(
      '/integrations/ifood/catalog/status',
      { items },
    )
    return data
  },

  async getBatch(batchId: string): Promise<IfoodCatalogBatchStatus> {
    const { data } = await api.get<IfoodCatalogBatchStatus>(
      `/integrations/ifood/catalog/batch/${batchId}`,
    )
    return data
  },
}
