export type IngredientStatus = 'ACTIVE' | 'INACTIVE'

export interface IngredientRequest {
  name: string
  unit: string
  costPerUnit: number
}

export interface IngredientResponse {
  id: string
  name: string
  unit: string
  costPerUnit: number
  status: IngredientStatus
}

