<script setup lang="ts">
import { useAttrs } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    modelValue?: string | number
    placeholder?: string
    width?: string | number
    disabled?: boolean
  }>(),
  { width: '100%' },
)

defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const attrs = useAttrs()
</script>

<template>
  <div
    class="ui-input"
    :style="{
      display: 'flex',
      alignItems: 'center',
      gap: '8px',
      padding: '5px 12px',
      background: UI.panel,
      border: `1px solid ${UI.border}`,
      borderRadius: '9px',
      fontSize: '13px',
      width: typeof width === 'number' ? width + 'px' : width,
      position: 'relative',
      color: UI.text,
    }"
  >
    <select
      v-bind="attrs"
      :value="modelValue"
      :disabled="disabled"
      style="flex: 1; border: none; outline: none; background: transparent; font: inherit; color: inherit; appearance: none; padding: 4px 0; cursor: pointer; min-width: 0;"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option v-if="placeholder" value="" disabled>{{ placeholder }}</option>
      <slot />
    </select>
    <UIIcon name="chevDown" :size="13" />
  </div>
</template>

<style scoped>
.ui-input {
  transition: border-color 0.12s ease;
}
.ui-input:hover {
  border-color: #94a3b8;
}
</style>
