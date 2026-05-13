export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export interface ProductRequest {
  name: string
  price: number
}

export interface ProductResponse {
  id: string
  name: string
  price: number
  estimatedCost: number | null
  margin: number | null
  status: ProductStatus
  cmv: number | null
}

export interface RecipeItemRequest {
  ingredientId: string
  quantity: number
}

export interface RecipeItemResponse {
  id: string
  productId: string
  ingredientId: string
  ingredientName: string
  ingredientUnit: string
  quantity: number
  costPerUnit: number
  totalCost: number
}

