import { describe, it, expect, afterEach } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import UIModal from '@/design/UIModal.vue'

enableAutoUnmount(afterEach)

afterEach(() => {
  document.body.style.overflow = ''
})

function mountModal() {
  return mount(UIModal, {
    props: { title: 'Título do Modal' },
    slots: { default: '<input data-testid="field" />' },
  })
}

describe('UIModal', () => {
  it('should render the title', () => {
    const wrapper = mountModal()

    expect(wrapper.text()).toContain('Título do Modal')
  })

  it('should NOT emit close when the backdrop is clicked', async () => {
    const wrapper = mountModal()

    await wrapper.get('.ui-modal-backdrop').trigger('click')

    expect(wrapper.emitted('close')).toBeUndefined()
  })

  it('should NOT emit close when a press starts inside and ends on the backdrop', async () => {
    const wrapper = mountModal()

    // Selecting text inside an input (mousedown) and releasing over the
    // backdrop (mouseup) makes the browser dispatch a click on the backdrop.
    await wrapper.get('[data-testid="field"]').trigger('mousedown')
    await wrapper.get('.ui-modal-backdrop').trigger('mouseup')
    await wrapper.get('.ui-modal-backdrop').trigger('click')

    expect(wrapper.emitted('close')).toBeUndefined()
  })

  it('should NOT emit close when the panel is clicked', async () => {
    const wrapper = mountModal()

    await wrapper.get('.ui-modal').trigger('click')

    expect(wrapper.emitted('close')).toBeUndefined()
  })

  it('should emit close when the X button is clicked', async () => {
    const wrapper = mountModal()

    await wrapper.get('[data-testid="ui-modal-close"]').trigger('click')

    expect(wrapper.emitted('close')).toHaveLength(1)
  })

  it('should lock body scroll while open and restore it on unmount', () => {
    document.body.style.overflow = ''

    const wrapper = mountModal()
    expect(document.body.style.overflow).toBe('hidden')

    wrapper.unmount()
    expect(document.body.style.overflow).toBe('')
  })
})
