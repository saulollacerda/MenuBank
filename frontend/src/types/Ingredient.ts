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
  defaultQuantity?: number
  status: IngredientStatus
  ingredientCategoryId: string | null
  ingredientCategoryName: string | null
}
