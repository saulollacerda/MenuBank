<script setup lang="ts">
import { useAttrs } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    width?: number
    titleTestId?: string
  }>(),
  { width: 560 },
)

defineEmits<{ (e: 'close'): void }>()

const attrs = useAttrs()
</script>

<template>
  <div
    v-bind="attrs"
    class="ui-modal-backdrop"
    :style="{
      position: 'fixed',
      inset: 0,
      background: 'rgba(15,23,42,0.45)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
      padding: '24px',
    }"
    @click.self="$emit('close')"
  >
    <div
      :style="{
        background: UI.panel,
        borderRadius: '14px',
        width: width + 'px',
        maxWidth: '100%',
        maxHeight: '95vh',
        display: 'flex',
        flexDirection: 'column',
        boxShadow: '0 20px 60px rgba(0,0,0,0.25), 0 0 0 1px rgba(255,255,255,0.05)',
      }"
    >
      <div
        :style="{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '18px 22px',
          borderBottom: `1px solid ${UI.border}`,
        }"
      >
        <div>
          <div
            :data-testid="titleTestId"
            :style="{
              fontSize: '16px',
              fontWeight: 700,
              color: UI.text,
              letterSpacing: '-0.2px',
            }"
          >
            {{ title }}
          </div>
          <div
            v-if="subtitle"
            :style="{ fontSize: '12px', color: UI.textSub, marginTop: '3px' }"
          >
            {{ subtitle }}
          </div>
        </div>
        <div
          :style="{
            width: '30px',
            height: '30px',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: UI.textSub,
            cursor: 'pointer',
            background: UI.bg,
          }"
          @click="$emit('close')"
        >
          <UIIcon name="x" :size="14" />
        </div>
      </div>
      <div :style="{ padding: '20px 22px', flex: 1, overflow: 'auto' }">
        <slot />
      </div>
      <div
        v-if="$slots.footer"
        :style="{
          display: 'flex',
          justifyContent: 'flex-end',
          gap: '8px',
          padding: '14px 22px',
          borderTop: `1px solid ${UI.border}`,
          background: UI.bgSoft,
          borderRadius: '0 0 14px 14px',
          alignItems: 'center',
        }"
      >
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>
