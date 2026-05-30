<script setup lang="ts">
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'

withDefaults(
  defineProps<{ modelValue?: string; placeholder?: string; width?: string | number }>(),
  { placeholder: 'Buscar…', width: 320 },
)

defineEmits<{ (e: 'update:modelValue', v: string): void }>()
</script>

<template>
  <div
    class="ui-input"
    :style="{
      display: 'flex',
      alignItems: 'center',
      gap: '8px',
      padding: '8px 12px',
      background: UI.panel,
      border: `1px solid ${UI.border}`,
      borderRadius: '9px',
      width: typeof width === 'number' ? width + 'px' : width,
      color: UI.textSub,
      fontSize: '12.5px',
    }"
  >
    <UIIcon name="search" :size="14" />
    <input
      :value="modelValue"
      :placeholder="placeholder"
      style="flex: 1; border: none; outline: none; background: transparent; font: inherit; color: inherit; min-width: 0;"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
  </div>
</template>

<style scoped>
.ui-input {
  transition: border-color 0.12s ease;
}
.ui-input:hover,
.ui-input:focus-within {
  border-color: #94a3b8;
}
</style>
