<script setup lang="ts">
import { computed } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'

type Color = 'gray' | 'blue' | 'rose' | 'emerald'

const props = withDefaults(
  defineProps<{ icon: string; color?: Color; label?: string }>(),
  { color: 'gray' },
)

defineEmits<{ (e: 'click', ev: MouseEvent): void }>()

const palette = computed(() => {
  const map: Record<Color, { bg: string; fg: string }> = {
    gray: { bg: UI.bg, fg: UI.text },
    blue: { bg: UI.blueBg, fg: UI.blue },
    rose: { bg: UI.roseBg, fg: UI.rose },
    emerald: { bg: UI.emeraldBg, fg: UI.emerald2 },
  }
  return map[props.color]
})
</script>

<template>
  <div
    :title="label"
    class="ui-row-action"
    :style="{
      width: '30px',
      height: '30px',
      borderRadius: '7px',
      background: palette.bg,
      color: palette.fg,
      display: 'inline-flex',
      alignItems: 'center',
      justifyContent: 'center',
      cursor: 'pointer',
    }"
    @click="(e) => $emit('click', e)"
  >
    <UIIcon :name="icon" :size="14" />
  </div>
</template>

<style scoped>
.ui-row-action {
  transition: transform 0.15s ease, background 0.15s ease;
}
.ui-row-action:hover {
  transform: scale(1.08);
}
</style>
