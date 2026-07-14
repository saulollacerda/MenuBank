import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useToast } from '@/composables/useToast'

describe('useToast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    useToast().toasts.value.splice(0)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should add a success toast with the given message by default', () => {
    const { showToast, toasts } = useToast()

    showToast('Ingrediente criado com sucesso!')

    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0]!.message).toBe('Ingrediente criado com sucesso!')
    expect(toasts.value[0]!.variant).toBe('success')
  })

  it('should support an error variant', () => {
    const { showToast, toasts } = useToast()

    showToast('Erro ao salvar', 'error')

    expect(toasts.value[0]!.variant).toBe('error')
  })

  it('should share state across composable instances', () => {
    useToast().showToast('Pedido criado com sucesso!')

    expect(useToast().toasts.value).toHaveLength(1)
  })

  it('should auto-dismiss a toast after the default duration', () => {
    const { showToast, toasts } = useToast()

    showToast('Produto criado com sucesso!')
    expect(toasts.value).toHaveLength(1)

    vi.advanceTimersByTime(3500)

    expect(toasts.value).toHaveLength(0)
  })

  it('should dismiss a toast by id', () => {
    const { showToast, dismissToast, toasts } = useToast()

    showToast('Primeiro')
    showToast('Segundo')
    const firstId = toasts.value[0]!.id

    dismissToast(firstId)

    expect(toasts.value).toHaveLength(1)
    expect(toasts.value[0]!.message).toBe('Segundo')
  })

  it('should assign unique ids to concurrent toasts', () => {
    const { showToast, toasts } = useToast()

    showToast('Um')
    showToast('Dois')

    expect(toasts.value[0]!.id).not.toBe(toasts.value[1]!.id)
  })
})
