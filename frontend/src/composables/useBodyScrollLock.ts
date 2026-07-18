import { onMounted, onUnmounted } from 'vue'

/**
 * Body scroll lock with reference counting so that stacked modals share a
 * single lock: the body is only unlocked once every locker has released it.
 */
let lockCount = 0
let previousOverflow = ''

export function lockBodyScroll(): void {
  if (lockCount === 0) {
    previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  }
  lockCount += 1
}

export function unlockBodyScroll(): void {
  if (lockCount === 0) return
  lockCount -= 1
  if (lockCount === 0) {
    document.body.style.overflow = previousOverflow
  }
}

/**
 * Locks page scroll while the calling component is mounted and restores it on
 * unmount. Use inside modal components so long content scrolls within the modal
 * panel instead of the page behind it.
 */
export function useBodyScrollLock(): void {
  onMounted(lockBodyScroll)
  onUnmounted(unlockBodyScroll)
}
