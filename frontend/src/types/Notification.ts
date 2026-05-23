export type NotificationType = 'MISSING_INGREDIENT'

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
