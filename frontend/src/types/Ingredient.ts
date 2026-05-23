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
}

export interface IngredientCostRequest {
  costPerUnit: number
  defaultQuantity?: number
  unit?: string
}
