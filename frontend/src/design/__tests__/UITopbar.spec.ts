import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

vi.mock('@/components/NotificationBell.vue', () => ({
  default: { template: '<div data-testid="bell-stub" />' },
}))

import UITopbar from '@/design/UITopbar.vue'
import { useSidebar } from '@/composables/useSidebar'

describe('UITopbar — hambúrguer mobile', () => {
  beforeEach(() => {
    useSidebar().close()
  })

  it('renderiza o botão hambúrguer', () => {
    const wrapper = mount(UITopbar, { props: { title: 'Pedidos' } })
    expect(wrapper.find('[data-testid="sidebar-hamburger"]').exists()).toBe(true)
  })

  it('clicar no hambúrguer abre o drawer da sidebar', async () => {
    const wrapper = mount(UITopbar, { props: { title: 'Pedidos' } })
    await wrapper.find('[data-testid="sidebar-hamburger"]').trigger('click')
    expect(useSidebar().isOpen.value).toBe(true)
  })

  it('mantém título e subtítulo', () => {
    const wrapper = mount(UITopbar, {
      props: { title: 'Pedidos', subtitle: '12 pedidos no total' },
    })
    expect(wrapper.text()).toContain('Pedidos')
    expect(wrapper.text()).toContain('12 pedidos no total')
  })

  it('não renderiza a pill estática de período', () => {
    const wrapper = mount(UITopbar, { props: { title: 'Pedidos' } })
    expect(wrapper.find('.ui-topbar-pill').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Maio 2026')
  })
})
