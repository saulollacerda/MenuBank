import api from './api'
import type { NotificationResponse, UnreadCountResponse } from '@/types/Notification'
import type { Page, PageParams } from '@/types/Page'
import { DEFAULT_PAGE_SIZE } from '@/types/Page'

export const notificationService = {
  async findAll(params: PageParams = {}): Promise<Page<NotificationResponse>> {
    const { data } = await api.get<Page<NotificationResponse>>('/notifications', {
      params: {
        page: params.page ?? 0,
        size: params.size ?? DEFAULT_PAGE_SIZE,
      },
    })
    return data
  },

  async unreadCount(): Promise<number> {
    const { data } = await api.get<UnreadCountResponse>('/notifications/unread-count')
    return data.count
  },

  async markRead(id: string): Promise<void> {
    await api.put(`/notifications/${id}/read`)
  },

  async dismiss(id: string): Promise<void> {
    await api.delete(`/notifications/${id}`)
  },
}
