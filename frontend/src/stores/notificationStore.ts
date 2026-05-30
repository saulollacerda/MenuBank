import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { NotificationResponse } from '@/types/Notification'
import { notificationService } from '@/services/notificationService'

export const useNotificationStore = defineStore('notification', () => {
  const items = ref<NotificationResponse[]>([])
  const unreadCount = ref(0)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      const result = await notificationService.findAll({ page: 0, size: 20 })
      items.value = result.content
    } catch (e: unknown) {
      error.value = 'Erro ao carregar notificações'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function refreshCount() {
    try {
      unreadCount.value = await notificationService.unreadCount()
    } catch {
      // silent failure for background polling
    }
  }

  async function markRead(id: string) {
    await notificationService.markRead(id)
    const target = items.value.find((n) => n.id === id)
    if (target && target.status === 'UNREAD') {
      target.status = 'READ'
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  async function dismiss(id: string) {
    await notificationService.dismiss(id)
    const idx = items.value.findIndex((n) => n.id === id)
    if (idx !== -1) {
      const removed = items.value.splice(idx, 1)[0]
      if (removed?.status === 'UNREAD') {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    }
  }

  return {
    items,
    unreadCount,
    loading,
    error,
    fetchAll,
    refreshCount,
    markRead,
    dismiss,
  }
})
