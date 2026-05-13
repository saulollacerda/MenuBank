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
  defaultQuantity?: number
  status: IngredientStatus
}
