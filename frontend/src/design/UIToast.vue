<script setup lang="ts">
import { useToast } from '@/composables/useToast'
import { UI } from './tokens'

const { toasts, dismissToast } = useToast()
</script>

<template>
  <div class="toast-container" aria-live="polite">
    <TransitionGroup name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast"
        :class="t.variant"
        role="status"
        data-testid="toast"
        @click="dismissToast(t.id)"
      >
        <span class="toast-icon">{{ t.variant === 'success' ? '✓' : '✕' }}</span>
        {{ t.message }}
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
}
.toast {
  pointer-events: auto;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 10px;
  font-family: v-bind('UI.font');
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  border: 1px solid v-bind('UI.border');
}
.toast.success {
  background: v-bind('UI.emeraldBg');
  color: v-bind('UI.emerald2');
}
.toast.error {
  background: v-bind('UI.roseBg');
  color: v-bind('UI.rose2');
}
.toast-icon {
  font-size: 13px;
}
.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.2s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
