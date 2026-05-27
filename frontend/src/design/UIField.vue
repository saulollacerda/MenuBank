<script setup lang="ts">
import { UI } from './tokens'

withDefaults(defineProps<{ label?: string; hint?: string; width?: string | number }>(), {
  width: '100%',
})
</script>

<template>
  <label :style="{ display: 'block', width: typeof width === 'number' ? width + 'px' : width }">
    <div
      v-if="label || $slots.label"
      :style="{
        fontSize: '12px',
        color: UI.text,
        fontWeight: 600,
        marginBottom: '6px',
      }"
    >
      <slot name="label">{{ label }}</slot>
    </div>
    <slot />
    <div
      v-if="hint"
      :style="{ fontSize: '11px', color: UI.textMute, marginTop: '5px' }"
    >
      {{ hint }}
    </div>
  </label>
</template>
