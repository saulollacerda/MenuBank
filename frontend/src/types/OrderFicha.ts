/**
 * Ficha do pedido: insumos cobrados UMA vez por pedido (sacola, guardanapo), independentemente
 * da quantidade de itens. Diferente da ficha técnica do produto (includes), que é por item e
 * multiplicada pela quantidade — 2 copos consomem 2 copos, mas saem numa sacola só.
 */

/** Uma linha da configuração: ingrediente + quantidade consumida por pedido. */
export interface OrderFichaLineRequest {
  ingredientId: string
  quantity: number
}

export interface OrderFichaRequest {
  lines: OrderFichaLineRequest[]
}

export interface OrderFichaLineResponse {
  id: string
  ingredientId: string
  ingredientName: string
  ingredientUnit: string
  quantity: number
  costPerUnit: number
  /** `quantity × costPerUnit` — quanto esta linha custa em cada pedido. */
  totalCost: number
}

export interface OrderFichaResponse {
  lines: OrderFichaLineResponse[]
  /** Soma das linhas: custo aplicado uma vez em cada pedido. */
  totalCost: number
}

/** Linha da ficha como gravada NO pedido (snapshot), exibida no detalhe do pedido. */
export interface OrderFichaIngredientResponse {
  id: string
  ingredientId: string
  ingredientName: string
  ingredientUnit: string
  quantity: number
  costPerUnit: number
  totalCost: number
}
