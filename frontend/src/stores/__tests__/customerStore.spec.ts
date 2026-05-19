import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCustomerStore } from '@/stores/customerStore'
import { customerService } from '@/services/customerService'

vi.mock('@/services/customerService')

const mockedService = vi.mocked(customerService)

function asPage<T>(content: T[], size = 20) {
  return {
    content,
    totalElements: content.length,
    totalPages: content.length === 0 ? 0 : 1,
    number: 0,
    size,
    first: true,
    last: true,
    empty: content.length === 0,
  }
}

describe('customerStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchPage should populate items and pagination state', async () => {
    const mockData = [{ id: '1', name: 'João', phone: '11999', email: 'j@test.com' }]
    mockedService.findAll.mockResolvedValue(asPage(mockData))

    const store = useCustomerStore()
    await store.fetchPage({ search: 'joão' })

    expect(store.items).toEqual(mockData)
    expect(store.search).toBe('joão')
    expect(store.totalElements).toBe(1)
    expect(store.loading).toBe(false)
  })

  it('create should refetch the current page', async () => {
    const created = { id: '1', name: 'João', phone: '11999', email: 'j@test.com' }
    mockedService.create.mockResolvedValue(created)
    mockedService.findAll.mockResolvedValue(asPage([created]))

    const store = useCustomerStore()
    await store.create({ name: 'João', phone: '11999', email: 'j@test.com' })

    expect(store.items).toContainEqual(created)
  })

  it('update should replace existing item', async () => {
    const store = useCustomerStore()
    store.items = [{ id: '1', name: 'João', phone: '11999', email: 'j@test.com' }]

    const updated = { id: '1', name: 'João Silva', phone: '11999', email: 'j@test.com' }
    mockedService.update.mockResolvedValue(updated)

    await store.update('1', { name: 'João Silva' })

    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    expect(store.items[0]!.name).toBe('João Silva')
  })

  it('remove should call service and refetch the current page', async () => {
    const remaining = { id: '2', name: 'Maria', phone: '11888', email: 'm@test.com' }
    mockedService.remove.mockResolvedValue()
    mockedService.findAll.mockResolvedValue(asPage([remaining]))

    const store = useCustomerStore()
    await store.remove('1')

    expect(mockedService.remove).toHaveBeenCalledWith('1')
    expect(store.items).toEqual([remaining])
  })
})
