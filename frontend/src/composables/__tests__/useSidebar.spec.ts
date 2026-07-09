import { describe, it, expect, beforeEach } from 'vitest'
import { useSidebar } from '../useSidebar'

describe('useSidebar', () => {
  beforeEach(() => {
    useSidebar().close()
  })

  it('starts closed', () => {
    const { isOpen } = useSidebar()
    expect(isOpen.value).toBe(false)
  })

  it('toggle opens and closes', () => {
    const { isOpen, toggle } = useSidebar()
    toggle()
    expect(isOpen.value).toBe(true)
    toggle()
    expect(isOpen.value).toBe(false)
  })

  it('close is idempotent', () => {
    const { isOpen, toggle, close } = useSidebar()
    toggle()
    close()
    close()
    expect(isOpen.value).toBe(false)
  })

  it('shares state between callers (singleton)', () => {
    const a = useSidebar()
    const b = useSidebar()
    a.toggle()
    expect(b.isOpen.value).toBe(true)
  })
})
