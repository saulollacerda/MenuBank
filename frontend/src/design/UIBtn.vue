<script setup lang="ts">
import { computed } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'

type Variant = 'primary' | 'dark' | 'emerald' | 'secondary' | 'ghost' | 'danger' | 'softDanger'
type Size = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    variant?: Variant
    size?: Size
    icon?: string
    danger?: boolean
    disabled?: boolean
    type?: 'button' | 'submit'
  }>(),
  { variant: 'secondary', size: 'md', type: 'button' },
)

defineEmits<{ (e: 'click', ev: MouseEvent): void }>()

const sizeStyle = computed(() => {
  const map: Record<Size, { padding: string; fontSize: string }> = {
    sm: { padding: '6px 11px', fontSize: '12px' },
    md: { padding: '8px 14px', fontSize: '12.5px' },
    lg: { padding: '10px 18px', fontSize: '13px' },
  }
  return map[props.size]
})

const variantStyle = computed(() => {
  const map: Record<Variant, { background: string; color: string; border: string }> = {
    primary: { background: UI.blue, color: '#fff', border: `1px solid ${UI.blue}` },
    dark: { background: UI.text, color: '#fff', border: `1px solid ${UI.text}` },
    emerald: { background: UI.emerald2, color: '#fff', border: `1px solid ${UI.emerald2}` },
    secondary: { background: UI.panel, color: UI.text, border: `1px solid ${UI.border}` },
    ghost: { background: 'transparent', color: UI.text, border: '1px solid transparent' },
    danger: { background: UI.rose, color: '#fff', border: `1px solid ${UI.rose}` },
    softDanger: { background: UI.roseBg, color: UI.rose, border: `1px solid ${UI.roseBg}` },
  }
  return map[props.danger ? 'danger' : props.variant]
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled"
    class="ui-btn"
    :style="{
      ...sizeStyle,
      ...variantStyle,
      borderRadius: '9px',
      fontWeight: 600,
      display: 'inline-flex',
      alignItems: 'center',
      gap: '7px',
      cursor: disabled ? 'not-allowed' : 'pointer',
      whiteSpace: 'nowrap',
      userSelect: 'none',
      opacity: disabled ? 0.55 : 1,
      fontFamily: 'inherit',
    }"
    @click="(e) => $emit('click', e)"
  >
    <UIIcon v-if="icon" :name="icon" :size="14" />
    <slot />
  </button>
</template>

<style scoped>
.ui-btn {
  transition: transform 0.14s ease, box-shadow 0.14s ease, filter 0.12s ease;
}
.ui-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.1);
  filter: brightness(1.04);
}
.ui-btn:active:not(:disabled) {
  transform: translateY(0);
  filter: brightness(0.96);
}
</style>
