import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useNotificationStore } from '@/stores/notificationStore'
import { notificationService } from '@/services/notificationService'
import type { NotificationResponse } from '@/types/Notification'

vi.mock('@/services/notificationService')

const mockedService = vi.mocked(notificationService)

function buildNotification(overrides: Partial<NotificationResponse> = {}): NotificationResponse {
  return {
    id: 'n-1',
    type: 'MISSING_INGREDIENT',
    title: 'Ingrediente não cadastrado',
    message: "O ingrediente 'Pistache' apareceu em um pedido mas não está cadastrado.",
    referenceData: 'pistache',
    referenceDisplay: 'Pistache',
    status: 'UNREAD',
    createdAt: '2026-05-22T12:00:00Z',
    resolvedAt: null,
    ...overrides,
  }
}

function asPage<T>(content: T[]) {
  return {
    content,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    number: 0,
    size: 20,
    first: true,
    last: true,
    empty: content.length === 0,
  }
}

describe('notificationStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchAll populates items', async () => {
    const items = [buildNotification(), buildNotification({ id: 'n-2', status: 'READ' })]
    mockedService.findAll.mockResolvedValue(asPage(items))

    const store = useNotificationStore()
    await store.fetchAll()

    expect(store.items).toEqual(items)
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('refreshCount fetches unread count', async () => {
    mockedService.unreadCount.mockResolvedValue(7)

    const store = useNotificationStore()
    await store.refreshCount()

    expect(store.unreadCount).toBe(7)
  })

  it('refreshCount swallows errors silently', async () => {
    mockedService.unreadCount.mockRejectedValue(new Error('boom'))

    const store = useNotificationStore()
    store.unreadCount = 3
    await store.refreshCount()

    // Stays at previous value, no exception
    expect(store.unreadCount).toBe(3)
  })

  it('markRead decrements unreadCount and flips status to READ', async () => {
    const items = [buildNotification(), buildNotification({ id: 'n-2', status: 'READ' })]
    mockedService.findAll.mockResolvedValue(asPage(items))
    mockedService.markRead.mockResolvedValue()

    const store = useNotificationStore()
    await store.fetchAll()
    store.unreadCount = 1

    await store.markRead('n-1')

    expect(store.items[0]!.status).toBe('READ')
    expect(store.unreadCount).toBe(0)
    expect(mockedService.markRead).toHaveBeenCalledWith('n-1')
  })

  it('markRead does not decrement when item already READ', async () => {
    const items = [buildNotification({ status: 'READ' })]
    mockedService.findAll.mockResolvedValue(asPage(items))
    mockedService.markRead.mockResolvedValue()

    const store = useNotificationStore()
    await store.fetchAll()
    store.unreadCount = 0

    await store.markRead('n-1')

    expect(store.unreadCount).toBe(0)
  })

  it('dismiss removes item from list and decrements when UNREAD', async () => {
    const items = [buildNotification(), buildNotification({ id: 'n-2' })]
    mockedService.findAll.mockResolvedValue(asPage(items))
    mockedService.dismiss.mockResolvedValue()

    const store = useNotificationStore()
    await store.fetchAll()
    store.unreadCount = 2

    await store.dismiss('n-1')

    expect(store.items).toHaveLength(1)
    expect(store.items[0]!.id).toBe('n-2')
    expect(store.unreadCount).toBe(1)
  })
})
