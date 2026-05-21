export type IngredientStatus = 'ACTIVE' | 'INACTIVE'

export interface IngredientRequest {
  name: string
  unit: string
  costPerUnit: number
  defaultQuantity?: number
  ingredientCategoryId?: string | null
}

export interface IngredientResponse {
  id: string
  name: string
  unit: string
  costPerUnit: number
  salePrice?: number | null
  defaultQuantity?: number
  status: IngredientStatus
  ingredientCategoryId: string | null
  ingredientCategoryName: string | null
  externalId?: string | null
}

export interface IngredientCostRequest {
  costPerUnit: number
  defaultQuantity?: number
  unit?: string
}
