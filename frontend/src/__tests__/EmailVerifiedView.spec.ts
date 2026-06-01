import { describe, it, expect } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import EmailVerifiedView from '@/views/EmailVerifiedView.vue'

function mountView() {
  return mount(EmailVerifiedView, {
    global: {
      stubs: { RouterLink: RouterLinkStub },
    },
  })
}

describe('EmailVerifiedView', () => {
  it('informa que o email foi verificado', () => {
    const wrapper = mountView()
    expect(wrapper.text()).toContain('email foi verificado')
  })

  it('tem um botão que leva para a tela de login', () => {
    const wrapper = mountView()
    const link = wrapper.findComponent(RouterLinkStub)
    expect(link.exists()).toBe(true)
    expect(link.props('to')).toBe('/login')
    expect(link.text().toLowerCase()).toContain('login')
  })
})
