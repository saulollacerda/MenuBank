<script setup lang="ts">
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'
import NotificationBell from '@/components/NotificationBell.vue'
import { useSidebar } from '@/composables/useSidebar'

defineProps<{
  title: string
  subtitle?: string
}>()

const sidebar = useSidebar()
</script>

<template>
  <div class="ui-topbar" :style="{ borderBottom: `1px solid ${UI.border}`, background: UI.panel }">
    <button
      class="ui-topbar-hamburger"
      data-testid="sidebar-hamburger"
      title="Abrir menu"
      @click="sidebar.toggle()"
    >
      <UIIcon name="menu" :size="18" />
    </button>
    <div style="min-width: 0; flex: 1">
      <div class="ui-topbar-title" :style="{ color: UI.text }">
        {{ title }}
      </div>
      <div
        v-if="subtitle"
        class="ui-topbar-subtitle"
        :style="{ color: UI.textSub }"
      >
        {{ subtitle }}
      </div>
    </div>
    <div class="ui-topbar-actions">
      <slot name="actions" />
      <NotificationBell />
    </div>
  </div>
</template>

<style scoped>
.ui-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 20px 32px;
  flex-shrink: 0;
  flex-wrap: wrap;
}

.ui-topbar-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.ui-topbar-subtitle {
  font-size: 12.5px;
  margin-top: 3px;
}

.ui-topbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.ui-topbar-hamburger {
  display: none;
  background: transparent;
  border: 1px solid #e8eaee;
  border-radius: 8px;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  color: #0f172a;
  cursor: pointer;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .ui-topbar {
    padding: 12px 16px;
  }
  .ui-topbar-title {
    font-size: 18px;
  }
  .ui-topbar-hamburger {
    display: flex;
  }
}
</style>
