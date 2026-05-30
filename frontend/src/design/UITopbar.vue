<script setup lang="ts">
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'
import NotificationBell from '@/components/NotificationBell.vue'

withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    periodPill?: boolean
    periodLabel?: string
  }>(),
  { periodPill: true, periodLabel: 'Maio 2026' },
)
</script>

<template>
  <div
    :style="{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '20px 32px',
      borderBottom: `1px solid ${UI.border}`,
      background: UI.panel,
      flexShrink: 0,
    }"
  >
    <div style="min-width: 0">
      <div
        :style="{
          fontSize: '22px',
          fontWeight: 700,
          color: UI.text,
          letterSpacing: '-0.5px',
        }"
      >
        {{ title }}
      </div>
      <div
        v-if="subtitle"
        :style="{ fontSize: '12.5px', color: UI.textSub, marginTop: '3px' }"
      >
        {{ subtitle }}
      </div>
    </div>
    <div style="display: flex; align-items: center; gap: 10px">
      <div
        v-if="periodPill"
        :style="{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
          padding: '8px 14px',
          background: UI.bg,
          border: `1px solid ${UI.border}`,
          borderRadius: '9px',
          fontSize: '12.5px',
          color: UI.text,
          fontWeight: 500,
        }"
      >
        <UIIcon name="clock" :size="14" />
        {{ periodLabel }}
        <UIIcon name="chevDown" :size="11" />
      </div>
      <slot name="actions" />
      <NotificationBell />
    </div>
  </div>
</template>
