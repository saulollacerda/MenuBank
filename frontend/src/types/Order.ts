export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED'

export interface OrderItemRequest {
  productId: string
  quantity: number
}

export interface OrderItemResponse {
  id: string
  productId: string
  productName: string
  quantity: number
  unitPrice: number
}

export interface OrderRequest {
  customerId: string
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
  items: OrderItemResponse[]
}

