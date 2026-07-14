import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import UIToast from '@/design/UIToast.vue'
import { useToast } from '@/composables/useToast'

describe('UIToast', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    useToast().toasts.value.splice(0)
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('should render nothing when there are no toasts', () => {
    const wrapper = mount(UIToast)
    expect(wrapper.find('[data-testid="toast"]').exists()).toBe(false)
  })

  it('should render active toast messages', async () => {
    const wrapper = mount(UIToast)

    useToast().showToast('Ingrediente criado com sucesso!')
    await wrapper.vm.$nextTick()

    const toast = wrapper.get('[data-testid="toast"]')
    expect(toast.text()).toContain('Ingrediente criado com sucesso!')
  })

  it('should announce toasts politely to screen readers', async () => {
    const wrapper = mount(UIToast)

    useToast().showToast('Pedido criado com sucesso!')
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[aria-live="polite"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="toast"]').attributes('role')).toBe('status')
  })

  it('should dismiss the toast when clicked', async () => {
    const wrapper = mount(UIToast)

    useToast().showToast('Produto criado com sucesso!')
    await wrapper.vm.$nextTick()

    await wrapper.get('[data-testid="toast"]').trigger('click')

    expect(wrapper.find('[data-testid="toast"]').exists()).toBe(false)
  })
})
