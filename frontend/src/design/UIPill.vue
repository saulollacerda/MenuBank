<script setup lang="ts">
import { computed } from 'vue'
import { UI } from './tokens'

type Color = 'gray' | 'emerald' | 'blue' | 'amber' | 'rose' | 'violet' | 'dark'

const props = withDefaults(
  defineProps<{ color?: Color; size?: 'sm' | 'md'; dot?: boolean }>(),
  { color: 'gray', size: 'md', dot: false },
)

const palette = computed(() => {
  const map: Record<Color, { bg: string; fg: string }> = {
    gray: { bg: '#f1f5f9', fg: '#475569' },
    emerald: { bg: UI.emeraldBg, fg: UI.emerald2 },
    blue: { bg: UI.blueBg, fg: UI.blue2 },
    amber: { bg: UI.amberBg, fg: UI.amber2 },
    rose: { bg: UI.roseBg, fg: UI.rose2 },
    violet: { bg: UI.violetBg, fg: UI.violet },
    dark: { bg: '#0f172a', fg: '#fff' },
  }
  return map[props.color]
})

const sz = computed(() =>
  props.size === 'sm'
    ? { fontSize: '10px', padding: '2px 7px' }
    : { fontSize: '11px', padding: '3px 9px' },
)
</script>

<template>
  <span
    :style="{
      ...sz,
      background: palette.bg,
      color: palette.fg,
      borderRadius: '5px',
      fontWeight: 600,
      letterSpacing: '0.3px',
      display: 'inline-flex',
      alignItems: 'center',
      gap: '5px',
      whiteSpace: 'nowrap',
    }"
  >
    <span
      v-if="dot"
      :style="{
        width: '6px',
        height: '6px',
        borderRadius: '3px',
        background: palette.fg,
      }"
    />
    <slot />
  </span>
</template>
