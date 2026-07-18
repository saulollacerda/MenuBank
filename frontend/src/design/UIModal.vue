<script setup lang="ts">
import { useAttrs } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'
import { useBodyScrollLock } from '@/composables/useBodyScrollLock'

defineOptions({ inheritAttrs: false })

// Lock page scroll while the modal is mounted so long content scrolls inside
// the panel instead of the page behind it.
useBodyScrollLock()

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
  <!-- Intentionally no backdrop @click handler: the modal must close only via
       the explicit X button so accidental backdrop clicks (or a text selection
       that starts inside and ends outside) never discard form state. -->
  <div
    v-bind="attrs"
    class="ui-modal-backdrop"
  >
    <div
      class="ui-modal"
      :style="{
        background: UI.panel,
        width: width + 'px',
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
          data-testid="ui-modal-close"
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

<style scoped>
.ui-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  padding: 24px;
}

.ui-modal {
  border-radius: 14px;
  max-width: 100%;
  max-height: 95vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25), 0 0 0 1px rgba(255, 255, 255, 0.05);
}

@media (max-width: 768px) {
  .ui-modal-backdrop {
    padding: 10px;
  }
  .ui-modal {
    max-height: calc(100dvh - 20px);
  }
}
</style>
