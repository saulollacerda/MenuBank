<script setup lang="ts">
import { UI } from './tokens'

withDefaults(
  defineProps<{ columns: string; gap?: number; header?: boolean; last?: boolean }>(),
  { gap: 12, header: false, last: false },
)
</script>

<template>
  <div
    v-if="header"
    :style="{
      display: 'grid',
      gridTemplateColumns: columns,
      gap: gap + 'px',
      padding: '12px 18px',
      background: UI.bgSoft,
      borderBottom: `1px solid ${UI.border}`,
      fontSize: '10.5px',
      color: UI.textSub,
      fontWeight: 600,
      textTransform: 'uppercase',
      letterSpacing: '0.5px',
    }"
  >
    <slot />
  </div>
  <div
    v-else
    class="ui-row"
    :style="{
      display: 'grid',
      gridTemplateColumns: columns,
      gap: gap + 'px',
      padding: '12px 18px',
      borderBottom: last ? 'none' : `1px solid ${UI.borderSub}`,
      fontSize: '13px',
      color: UI.text,
      alignItems: 'center',
    }"
  >
    <slot />
  </div>
</template>

<style scoped>
.ui-row {
  transition: background 0.12s ease;
}
.ui-row:hover {
  background: rgba(15, 23, 42, 0.025);
}
</style>
