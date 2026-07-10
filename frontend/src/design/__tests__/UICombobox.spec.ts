import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import UICombobox from '@/design/UICombobox.vue'

const OPTIONS = [
  { id: 'c1', label: 'João' },
  { id: 'c2', label: 'Pedro' },
  { id: 'c3', label: 'Maria Clara' },
]

function mountBox(props: Record<string, unknown> = {}) {
  return mount(UICombobox, {
    props: {
      modelValue: '',
      query: '',
      options: OPTIONS,
      placeholder: 'Selecione ou digite…',
      'data-testid': 'combo-input',
      ...props,
    },
    attachTo: document.body,
  })
}

describe('UICombobox', () => {
  it('renderiza o placeholder e o texto da query no input', () => {
    const wrapper = mountBox({ query: 'Jo' })
    const input = wrapper.find('[data-testid="combo-input"]')
    expect(input.attributes('placeholder')).toBe('Selecione ou digite…')
    expect((input.element as HTMLInputElement).value).toBe('Jo')
  })

  it('filtra opções ignorando caixa e acentos', async () => {
    const wrapper = mountBox({ query: 'joao' })
    await wrapper.find('[data-testid="combo-input"]').trigger('focus')

    const rows = wrapper.findAll('[role="option"]')
    expect(rows.map((r) => r.text())).toContain('João')
    expect(rows.some((r) => r.text().includes('Pedro'))).toBe(false)
  })

  it('clicar em uma opção emite id + label e fecha a lista', async () => {
    const wrapper = mountBox({ query: 'jo' })
    await wrapper.find('[data-testid="combo-input"]').trigger('focus')

    await wrapper.get('[data-testid="combo-option-c1"]').trigger('mousedown')
    await wrapper.get('[data-testid="combo-option-c1"]').trigger('click')

    expect(wrapper.emitted('update:modelValue')?.slice(-1)[0]).toEqual(['c1'])
    expect(wrapper.emitted('update:query')?.slice(-1)[0]).toEqual(['João'])
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)
  })

  it('digitar após uma seleção limpa o id selecionado', async () => {
    const wrapper = mountBox({ modelValue: 'c1', query: 'João' })
    await wrapper.find('[data-testid="combo-input"]').setValue('João P')

    expect(wrapper.emitted('update:modelValue')?.slice(-1)[0]).toEqual([''])
    expect(wrapper.emitted('update:query')?.slice(-1)[0]).toEqual(['João P'])
  })

  it('mostra a linha "criar" quando não há match exato e a oculta quando há', async () => {
    const wrapper = mountBox({ query: 'Mar' })
    await wrapper.find('[data-testid="combo-input"]').trigger('focus')
    expect(wrapper.find('[data-testid="combo-create"]').text()).toContain('Mar')

    await wrapper.setProps({ query: 'joão' })
    expect(wrapper.find('[data-testid="combo-create"]').exists()).toBe(false)
  })

  it('selecionar a linha "criar" mantém o texto livre e fecha a lista', async () => {
    const wrapper = mountBox({ query: 'Mar' })
    await wrapper.find('[data-testid="combo-input"]').trigger('focus')

    await wrapper.get('[data-testid="combo-create"]').trigger('mousedown')
    await wrapper.get('[data-testid="combo-create"]').trigger('click')

    expect(wrapper.emitted('update:modelValue')?.slice(-1)[0]).toEqual([''])
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)
  })

  it('ArrowDown + Enter seleciona a opção destacada sem submeter o form pai', async () => {
    const onSubmit = vi.fn()
    const wrapper = mount(
      {
        components: { UICombobox },
        template: `
          <form @submit.prevent="onSubmit">
            <UICombobox model-value="" query="jo" :options="options" data-testid="combo-input" />
          </form>
        `,
        setup: () => ({ onSubmit, options: OPTIONS }),
      },
      { attachTo: document.body },
    )
    const input = wrapper.find('[data-testid="combo-input"]')
    await input.trigger('focus')
    await input.trigger('keydown', { key: 'ArrowDown' })
    await input.trigger('keydown', { key: 'Enter' })

    const combo = wrapper.findComponent(UICombobox)
    expect(combo.emitted('update:modelValue')?.slice(-1)[0]).toEqual(['c1'])
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('botão de limpar zera id e query', async () => {
    const wrapper = mountBox({ modelValue: 'c1', query: 'João' })
    await wrapper.get('[data-testid="combo-clear"]').trigger('click')

    expect(wrapper.emitted('update:modelValue')?.slice(-1)[0]).toEqual([''])
    expect(wrapper.emitted('update:query')?.slice(-1)[0]).toEqual([''])
  })

  it('Escape fecha a lista', async () => {
    const wrapper = mountBox({ query: 'jo' })
    const input = wrapper.find('[data-testid="combo-input"]')
    await input.trigger('focus')
    expect(wrapper.find('[role="listbox"]').exists()).toBe(true)

    await input.trigger('keydown', { key: 'Escape' })
    expect(wrapper.find('[role="listbox"]').exists()).toBe(false)
  })
})
