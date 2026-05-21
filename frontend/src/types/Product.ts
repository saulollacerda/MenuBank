export type ProductStatus = 'ACTIVE' | 'INACTIVE'

export interface ProductRequest {
  name: string
  price: number
  categoryId: string
}

export interface ProductComplementGroupResponse {
  id: string
  ingredientCategoryId: string
  ingredientCategoryName: string
  minRequired: number
  maxAllowed: number
}

export interface ProductResponse {
  id: string
  name: string
  price: number
  status: ProductStatus
  categoryId: string
  categoryName: string
  complementGroups?: ProductComplementGroupResponse[]
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
  ingredientCategoryId?: string | null
  grammage: number
  isOptional: boolean
  costPerUnit: number
  totalCost: number
}

/** @deprecated use ProductIngredientRequest */
export type RecipeItemRequest = ProductIngredientRequest

/** @deprecated use ProductIngredientResponse */
export type RecipeItemResponse = ProductIngredientResponse

