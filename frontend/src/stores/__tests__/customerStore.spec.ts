import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCustomerStore } from '@/stores/customerStore'
import { customerService } from '@/services/customerService'

vi.mock('@/services/customerService')

const mockedService = vi.mocked(customerService)

describe('customerStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('fetchAll should populate items', async () => {
    const mockData = [{ id: '1', name: 'João', phone: '11999', email: 'j@test.com' }]
    mockedService.findAll.mockResolvedValue(mockData)

    const store = useCustomerStore()
    await store.fetchAll()

    expect(store.items).toEqual(mockData)
    expect(store.loading).toBe(false)
  })

  it('fetchAll should not call service again when data is already loaded', async () => {
    const mockData = [{ id: '1', name: 'João', phone: '11999', email: 'j@test.com' }]
    mockedService.findAll.mockResolvedValue(mockData)

    const store = useCustomerStore()
    await store.fetchAll()
    await store.fetchAll()

    expect(mockedService.findAll).toHaveBeenCalledTimes(1)
    expect(store.items).toEqual(mockData)
  })

  it('create should add item to the list', async () => {
    const created = { id: '1', name: 'João', phone: '11999', email: 'j@test.com' }
    mockedService.create.mockResolvedValue(created)

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

  it('remove should filter out the item', async () => {
    const store = useCustomerStore()
    store.items = [
      { id: '1', name: 'João', phone: '11999', email: 'j@test.com' },
      { id: '2', name: 'Maria', phone: '11888', email: 'm@test.com' },
    ]
    mockedService.remove.mockResolvedValue()

    await store.remove('1')

    expect(store.items).toHaveLength(1)
    // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    expect(store.items[0]!.id).toBe('2')
  })
})
