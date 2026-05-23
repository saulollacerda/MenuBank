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

export interface ProductIngredientRequest {
  ingredientId: string
  grammage: number
  isOptional?: boolean
}

export interface ProductIngredientResponse {
  id: string
  productId: string
  ingredientId: string
  ingredientName: string
  ingredientUnit: string
  grammage: number
  isOptional: boolean
  costPerUnit: number
  totalCost: number
}

/** @deprecated use ProductIngredientRequest */
export type RecipeItemRequest = ProductIngredientRequest

/** @deprecated use ProductIngredientResponse */
export type RecipeItemResponse = ProductIngredientResponse
