export type OrderStatus = 'PENDING' | 'READY' | 'DELIVERED' | 'PAID' | 'CANCELLED' | 'TEST'
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
  /**
   * Preço unitário pago pelo cliente por este adicional. `0` = complemento base.
   * Ausente para extras sem preço conhecido (pedido manual/iFood ou importado antes da V16).
   */
  salePricePerUnit?: number | null
  /** Valor total pago pelo cliente por este adicional. Não confundir com `totalCost` (custo). */
  salePriceTotal?: number | null
}

export interface OrderItemRequest {
  productId: string
  quantity: number
  extraIngredients?: OrderItemExtraIngredientRequest[]
  /** Ids dos includes da ficha técnica desmarcados neste item. Vazio = ficha completa. */
  excludedIncludeIds?: string[]
}

export interface OrderItemInsumoResponse {
  id: string
  productId: string
  name: string
  cost: number
  quantity: number
  totalCost: number
}

export interface OrderItemResponse {
  id: string
  productId: string
  productName: string
  quantity: number
  unitPrice: number
  unitCost: number
  totalCost: number
  insumos?: OrderItemInsumoResponse[]
  extraIngredients?: OrderItemExtraIngredientResponse[]
  /** Ids dos includes da ficha técnica desmarcados neste item (pedido manual). */
  excludedIncludeIds?: string[]
}

export interface OrderRequest {
  /** Cliente existente. Opcional quando customerName é informado. */
  customerId?: string
  /** Fluxo rápido: o backend reutiliza um cliente com o mesmo nome ou cria um novo. */
  customerName?: string
  status?: OrderStatus
  feeId?: string
  origin?: OrderOrigin
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
  /** Margem (%) apurada pelo backend sobre (totalValue - deliveryFee). Null quando o subtotal é zero. */
  marginPct?: number | null
  deliveryFee?: number
  totalCost?: number
  feeId?: string
  feeName?: string
  feeRate?: number
  items: OrderItemResponse[]
  origin?: OrderOrigin
}

