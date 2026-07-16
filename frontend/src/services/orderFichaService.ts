import api from './api'
import type { OrderFichaRequest, OrderFichaResponse } from '@/types/OrderFicha'

const base = '/order-ficha'

export const orderFichaService = {
  async find(): Promise<OrderFichaResponse> {
    const { data } = await api.get<OrderFichaResponse>(base)
    return data
  },

  /** Substitui a ficha inteira. Lista vazia limpa a ficha (custo zero por pedido). */
  async replace(request: OrderFichaRequest): Promise<OrderFichaResponse> {
    const { data } = await api.put<OrderFichaResponse>(base, request)
    return data
  },
}
