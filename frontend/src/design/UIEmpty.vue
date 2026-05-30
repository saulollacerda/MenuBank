<script setup lang="ts">
import { computed } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'
import UIBtn from './UIBtn.vue'

type Accent = 'blue' | 'emerald' | 'amber' | 'violet'

const props = withDefaults(
  defineProps<{
    icon?: string
    title: string
    body?: string
    primary?: string
    secondary?: string
    accent?: Accent
  }>(),
  { icon: 'box', accent: 'blue' },
)

defineEmits<{ (e: 'primary'): void; (e: 'secondary'): void }>()

const palette = computed(() => {
  const map: Record<Accent, { bg: string; fg: string }> = {
    blue: { bg: UI.blueBg, fg: UI.blue },
    emerald: { bg: UI.emeraldBg, fg: UI.emerald2 },
    amber: { bg: UI.amberBg, fg: UI.amber },
    violet: { bg: UI.violetBg, fg: UI.violet },
  }
  return map[props.accent]
})
</script>

<template>
  <div
    :style="{
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '40px',
      textAlign: 'center',
      background: UI.panel,
      border: `1px dashed ${UI.border}`,
      borderRadius: '14px',
    }"
  >
    <div
      :style="{
        width: '84px',
        height: '84px',
        borderRadius: '22px',
        background: palette.bg,
        color: palette.fg,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: '22px',
        position: 'relative',
      }"
    >
      <UIIcon :name="icon" :size="38" />
    </div>
    <div
      :style="{
        fontSize: '19px',
        fontWeight: 700,
        color: UI.text,
        letterSpacing: '-0.4px',
        marginBottom: '8px',
      }"
    >
      {{ title }}
    </div>
    <div
      v-if="body"
      :style="{
        fontSize: '13.5px',
        color: UI.textSub,
        lineHeight: 1.6,
        maxWidth: '460px',
      }"
    >
      {{ body }}
    </div>
    <div style="display: flex; gap: 10px; margin-top: 22px">
      <UIBtn v-if="secondary" variant="secondary" @click="$emit('secondary')">{{ secondary }}</UIBtn>
      <UIBtn v-if="primary" variant="primary" icon="plus" @click="$emit('primary')">
        {{ primary }}
      </UIBtn>
    </div>
  </div>
</template>
