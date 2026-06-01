export type ProductStatus = 'ACTIVE' | 'INACTIVE'
export type IncludeKind = 'INGREDIENT' | 'PACKAGING'

export interface ProductRequest {
  name: string
  price: number
  categoryId: string
}

export interface ProductResponse {
  id: string
  name: string
  price: number
  status: ProductStatus
  categoryId: string
  categoryName: string
}

/**
 * Item da ficha técnica do produto (tabela `includes`).
 * Sem FK para `ingredients`: name/cost são armazenados direto, por produto.
 */
export interface IncludeRequest {
  name: string
  cost: number
  /** Opcional. Backend assume 1 quando ausente. */
  quantity?: number
  kind?: IncludeKind
}

export interface IncludeResponse {
  id: string
  productId: string
  name: string
  cost: number
  quantity: number
  totalCost: number
  kind: IncludeKind
}
