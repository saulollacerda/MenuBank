export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED'
export type OrderOrigin = 'MENUBANK' | 'ANOTA_AI' | 'IFOOD'

export interface OrderItemExtraIngredientRequest {
  ingredientId: string
  quantity: number
}

export interface OrderItemExtraIngredientResponse {
  id: string
  ingredientId: string
  ingredientName: string
  ingredientUnit: string
  quantity: number
  costPerUnit: number
  totalCost: number
}

export interface OrderItemRequest {
  productId: string
  quantity: number
  extraIngredients?: OrderItemExtraIngredientRequest[]
}

export interface OrderItemResponse {
  id: string
  productId: string
  productName: string
  quantity: number
  unitPrice: number
  unitCost: number
  totalCost: number
  extraIngredients?: OrderItemExtraIngredientResponse[]
}

export interface OrderRequest {
  customerId: string
  status?: OrderStatus
  paymentMethodId?: string
  items: OrderItemRequest[]
}

export interface OrderResponse {
  id: string
  dateTime: string
  customerId: string
  customerName: string
  status: OrderStatus
  totalValue: number
  estimatedProfit: number
  deliveryFee?: number
  totalCost?: number
  paymentMethodId?: string
  paymentMethodName?: string
  feeRate?: number
  items: OrderItemResponse[]
  origin?: OrderOrigin
}

