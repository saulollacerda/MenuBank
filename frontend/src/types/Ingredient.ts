export type IngredientStatus = 'ACTIVE' | 'INACTIVE'

export interface IngredientRequest {
  name: string
  unit: string
  costPerUnit: number
  defaultQuantity?: number
}

export interface IngredientResponse {
  id: string
  name: string
  unit: string
  costPerUnit: number
  salePrice?: number | null
  defaultQuantity?: number
  status: IngredientStatus
  /** ISO date-time (LocalDateTime). Null for legacy rows created before the column existed. */
  createdAt?: string | null
  /** Zero-based manual ordering index within the merchant (default listing order). */
  position?: number | null
}

export interface IngredientCostRequest {
  costPerUnit: number
  defaultQuantity?: number
  unit?: string
}

/**
 * Onde o ingrediente aparece (match por nome) nas fichas técnicas dos produtos.
 * `includeId` referencia a tabela `includes` (era `productIngredientId` no modelo antigo).
 */
export interface IngredientProductUsageResponse {
  includeId: string
  productId: string
  productName: string
  quantity: number
  cost: number
  totalCost: number
}
