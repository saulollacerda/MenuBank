export type NotificationType = 'MISSING_INGREDIENT' | 'MISSING_PRODUCT' | 'ORDER_CANCELLED'

export const NOTIFICATION_TYPE_LABELS: Record<NotificationType, string> = {
  MISSING_INGREDIENT: 'Ingrediente não cadastrado',
  MISSING_PRODUCT: 'Produto não cadastrado',
  ORDER_CANCELLED: 'Pedido cancelado',
}

export type NotificationStatus = 'UNREAD' | 'READ' | 'RESOLVED'

export interface NotificationResponse {
  id: string
  type: NotificationType
  title: string
  message: string
  referenceData: string | null
  referenceDisplay: string | null
  status: NotificationStatus
  createdAt: string
  resolvedAt: string | null
}

export interface UnreadCountResponse {
  count: number
}
