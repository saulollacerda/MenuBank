import { describe, it, expect, afterEach } from 'vitest'
import { mount, enableAutoUnmount, RouterLinkStub } from '@vue/test-utils'
import LandingView from '@/views/LandingView.vue'

enableAutoUnmount(afterEach)

const GLOBAL = { global: { stubs: { RouterLink: RouterLinkStub } } }

describe('LandingView — pricing', () => {
  it('mostra apenas o plano Básico de R$ 50', () => {
    const wrapper = mount(LandingView, GLOBAL)

    const cards = wrapper.findAll('.lp-pricing-card')
    expect(cards).toHaveLength(1)
    expect(cards[0]!.text()).toContain('Básico')
    expect(cards[0]!.text()).toContain('R$ 50')
  })

  it('não mostra os planos fictícios (Inicial, Profissional, Multi-loja)', () => {
    const wrapper = mount(LandingView, GLOBAL)

    const text = wrapper.text()
    expect(text).not.toContain('R$ 79')
    expect(text).not.toContain('R$ 149')
    expect(text).not.toContain('R$ 299')
    expect(text).not.toContain('Profissional')
    expect(text).not.toContain('Multi-loja')
  })
})

describe('LandingView — apresentação pura (sem login/cadastro e sem teste grátis)', () => {
  it('não contém nenhum link para /login ou /register', () => {
    const wrapper = mount(LandingView, GLOBAL)

    const targets = wrapper
      .findAllComponents(RouterLinkStub)
      .map((link) => link.props('to'))
    expect(targets).not.toContain('/login')
    expect(targets).not.toContain('/register')

    const hrefs = wrapper.findAll('a').map((a) => a.attributes('href') ?? '')
    expect(hrefs).not.toContain('/login')
    expect(hrefs).not.toContain('/register')
  })

  it('direciona os CTAs para a página de planos', () => {
    const wrapper = mount(LandingView, GLOBAL)

    const targets = wrapper
      .findAllComponents(RouterLinkStub)
      .map((link) => link.props('to'))
    expect(targets).toContain('/planos')
  })

  it('não menciona teste grátis nem "sem cartão"', () => {
    const wrapper = mount(LandingView, GLOBAL)

    const text = wrapper.text()
    expect(text).not.toContain('sem cartão')
    expect(text).not.toContain('Sem cartão')
    expect(text).not.toContain('grátis')
    expect(text).not.toContain('7 dias')
  })
})

describe('LandingView — estatísticas', () => {
  it('não exibe a estatística de "350 lojas"', () => {
    const wrapper = mount(LandingView, GLOBAL)

    expect(wrapper.text()).not.toContain('350')
    expect(wrapper.text()).not.toContain('lojas usam o menubank')
  })
})

describe('LandingView — depoimento', () => {
  it('atribui a citação a Lucas Almeda, dono do Açaí Goat', () => {
    const wrapper = mount(LandingView, GLOBAL)

    const quote = wrapper.find('.lp-quote').text()
    expect(quote).toContain('Lucas Almeda')
    expect(quote).toContain('Dono do Açaí Goat')
    expect(quote).not.toContain('Renata Vasconcelos')
  })
})

describe('LandingView — rodapé', () => {
  it('exibe o CNPJ correto e o país sem cidade', () => {
    const wrapper = mount(LandingView, GLOBAL)

    const footer = wrapper.find('.lp-footer-bottom').text()
    expect(footer).toContain('67.595.605/0001-43')
    expect(footer).not.toContain('00.000.000/0001-00')
    expect(footer).not.toContain('São Paulo')
  })
})
