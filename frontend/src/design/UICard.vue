<script setup lang="ts">
import { UI } from './tokens'

withDefaults(defineProps<{ padding?: number }>(), { padding: 20 })
</script>

<template>
  <div
    :style="{
      background: UI.panel,
      border: `1px solid ${UI.border}`,
      borderRadius: '14px',
      padding: padding + 'px',
    }"
  >
    <slot />
  </div>
</template>
