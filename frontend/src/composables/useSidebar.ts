import { ref } from 'vue'

// Module-level state: the drawer is shared between UISidebar (renders it)
// and UITopbar (hamburger toggles it) without prop drilling or a store.
const isOpen = ref(false)

export function useSidebar() {
  const toggle = () => {
    isOpen.value = !isOpen.value
  }
  const close = () => {
    isOpen.value = false
  }
  return { isOpen, toggle, close }
}
