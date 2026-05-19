export type ProductStatus = 'ACTIVE' | 'INACTIVE'

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

