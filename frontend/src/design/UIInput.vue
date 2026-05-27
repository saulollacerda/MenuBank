<script setup lang="ts">
import { ref, useAttrs } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    modelValue?: string | number | null
    placeholder?: string
    type?: string
    icon?: string
    width?: string | number
    disabled?: boolean
  }>(),
  { type: 'text', width: '100%' },
)

defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const focused = ref(false)
const attrs = useAttrs()
</script>

<template>
  <div
    class="ui-input"
    :class="{ 'has-focus': focused }"
    :style="{
      display: 'flex',
      alignItems: 'center',
      gap: '8px',
      padding: '9px 12px',
      background: UI.panel,
      border: `1px solid ${UI.border}`,
      borderRadius: '9px',
      fontSize: '13px',
      color: UI.text,
      width: typeof width === 'number' ? width + 'px' : width,
    }"
  >
    <UIIcon v-if="icon" :name="icon" :size="14" />
    <input
      v-bind="attrs"
      :type="type"
      :value="modelValue ?? ''"
      :placeholder="placeholder"
      :disabled="disabled"
      style="flex: 1; border: none; outline: none; background: transparent; font: inherit; color: inherit; min-width: 0;"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @focus="focused = true"
      @blur="focused = false"
    />
    <slot name="rightAddon" />
  </div>
</template>

<style scoped>
.ui-input {
  transition: border-color 0.12s ease, box-shadow 0.12s ease;
}
.ui-input:hover {
  border-color: #94a3b8;
}
.ui-input.has-focus {
  border-color: #2563eb !important;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
</style>
