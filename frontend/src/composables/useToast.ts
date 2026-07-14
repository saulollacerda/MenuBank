import { ref } from 'vue'

export type ToastVariant = 'success' | 'error'

export interface Toast {
  id: number
  message: string
  variant: ToastVariant
}

const DEFAULT_DURATION_MS = 3500

// Module-level state: every useToast() caller shares the same toast list.
const toasts = ref<Toast[]>([])
let nextId = 0

export function useToast() {
  function dismissToast(id: number) {
    const idx = toasts.value.findIndex((t) => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  function showToast(
    message: string,
    variant: ToastVariant = 'success',
    durationMs = DEFAULT_DURATION_MS,
  ) {
    const id = nextId++
    toasts.value.push({ id, message, variant })
    setTimeout(() => dismissToast(id), durationMs)
  }

  return { toasts, showToast, dismissToast }
}
