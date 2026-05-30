import { onMounted, onUnmounted } from 'vue'

/** Runs `fn` on a fixed interval while the component is mounted. Does NOT call fn immediately. */
export function usePolling(fn: () => void, intervalMs: number): void {
  let timer: ReturnType<typeof setInterval> | null = null
  onMounted(() => { timer = setInterval(fn, intervalMs) })
  onUnmounted(() => { if (timer !== null) clearInterval(timer) })
}
