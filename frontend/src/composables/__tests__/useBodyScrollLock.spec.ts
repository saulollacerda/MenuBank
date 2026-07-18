import { describe, it, expect, beforeEach } from 'vitest'
import { lockBodyScroll, unlockBodyScroll } from '@/composables/useBodyScrollLock'

describe('useBodyScrollLock', () => {
  beforeEach(() => {
    // Ensure a clean baseline and drain any leftover lock counter.
    document.body.style.overflow = ''
    for (let i = 0; i < 10; i += 1) unlockBodyScroll()
    document.body.style.overflow = ''
  })

  it('should hide body overflow when a lock is acquired', () => {
    lockBodyScroll()

    expect(document.body.style.overflow).toBe('hidden')
  })

  it('should restore the previous overflow when the lock is released', () => {
    document.body.style.overflow = 'scroll'

    lockBodyScroll()
    expect(document.body.style.overflow).toBe('hidden')

    unlockBodyScroll()
    expect(document.body.style.overflow).toBe('scroll')
  })

  it('should keep the body locked while nested locks remain', () => {
    lockBodyScroll()
    lockBodyScroll()

    unlockBodyScroll()
    expect(document.body.style.overflow).toBe('hidden')

    unlockBodyScroll()
    expect(document.body.style.overflow).toBe('')
  })

  it('should ignore extra releases without a matching lock', () => {
    unlockBodyScroll()

    expect(document.body.style.overflow).toBe('')
  })
})
