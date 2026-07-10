<script setup lang="ts">
import { computed, ref, useAttrs } from 'vue'
import { UI } from './tokens'
import UIIcon from './UIIcon.vue'
import { normalizeText } from '@/lib/normalizeText'

defineOptions({ inheritAttrs: false })

export interface ComboOption {
  id: string
  label: string
}

const props = withDefaults(
  defineProps<{
    /** Id da opção selecionada; '' quando o texto é livre. */
    modelValue?: string
    /** Texto visível no input (v-model:query). */
    query?: string
    options: ComboOption[]
    placeholder?: string
    width?: string | number
    disabled?: boolean
  }>(),
  { modelValue: '', query: '', width: '100%' },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'update:query', value: string): void
}>()

const attrs = useAttrs()

const open = ref(false)
const highlighted = ref(-1)

const filtered = computed(() => {
  const canonical = normalizeText(props.query)
  if (!canonical) return props.options
  return props.options.filter((o) => normalizeText(o.label).includes(canonical))
})

/** Linha "criar": visível com texto não vazio e sem match canônico exato. */
const showCreate = computed(() => {
  const canonical = normalizeText(props.query)
  if (!canonical) return false
  return !props.options.some((o) => normalizeText(o.label) === canonical)
})

function openList() {
  if (props.disabled) return
  open.value = true
  highlighted.value = -1
}

function closeList() {
  open.value = false
  highlighted.value = -1
}

function onInput(event: Event) {
  const text = (event.target as HTMLInputElement).value
  if (props.modelValue) emit('update:modelValue', '')
  emit('update:query', text)
  openList()
}

function selectOption(option: ComboOption) {
  emit('update:modelValue', option.id)
  emit('update:query', option.label)
  closeList()
}

function selectCreate() {
  // Texto livre: mantém a query como está; o id fica vazio.
  emit('update:modelValue', '')
  closeList()
}

function clear() {
  emit('update:modelValue', '')
  emit('update:query', '')
  openList()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    closeList()
    return
  }
  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    event.preventDefault()
    if (!open.value) openList()
    const max = filtered.value.length - 1
    if (max < 0) return
    const delta = event.key === 'ArrowDown' ? 1 : -1
    highlighted.value = Math.min(Math.max(highlighted.value + delta, 0), max)
    return
  }
  if (event.key === 'Enter' && open.value) {
    // Só intercepta o Enter com a lista aberta — fora dela o form submete normal.
    event.preventDefault()
    const option = filtered.value[highlighted.value]
    if (option) {
      selectOption(option)
    } else if (showCreate.value) {
      selectCreate()
    }
  }
}
</script>

<template>
  <div
    :style="{
      position: 'relative',
      width: typeof width === 'number' ? width + 'px' : width,
    }"
  >
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
        color: UI.text,
      }"
    >
      <input
        v-bind="attrs"
        :value="query"
        :placeholder="placeholder"
        :disabled="disabled"
        type="text"
        role="combobox"
        :aria-expanded="open"
        autocomplete="off"
        style="flex: 1; border: none; outline: none; background: transparent; font: inherit; color: inherit; padding: 4px 0; min-width: 0;"
        @input="onInput"
        @focus="openList"
        @blur="closeList"
        @keydown="onKeydown"
      />
      <button
        v-if="query"
        type="button"
        data-testid="combo-clear"
        aria-label="Limpar"
        :style="{
          border: 'none',
          background: 'transparent',
          cursor: 'pointer',
          color: UI.textMute,
          display: 'flex',
          padding: 0,
        }"
        @mousedown.prevent
        @click="clear"
      >
        <UIIcon name="x" :size="12" />
      </button>
      <UIIcon v-else name="chevDown" :size="13" />
    </div>

    <div
      v-if="open && (filtered.length > 0 || showCreate)"
      role="listbox"
      :style="{
        position: 'absolute',
        top: 'calc(100% + 4px)',
        left: 0,
        right: 0,
        zIndex: 10,
        background: UI.panel,
        border: `1px solid ${UI.border}`,
        borderRadius: '9px',
        boxShadow: '0 8px 24px rgba(15,23,42,0.12)',
        maxHeight: '220px',
        overflowY: 'auto',
        padding: '4px',
      }"
    >
      <div
        v-for="(option, index) in filtered"
        :key="option.id"
        role="option"
        :aria-selected="option.id === modelValue"
        :data-testid="`combo-option-${option.id}`"
        :style="{
          padding: '7px 10px',
          borderRadius: '7px',
          cursor: 'pointer',
          fontSize: '13px',
          background:
            index === highlighted || option.id === modelValue ? UI.blueBg : 'transparent',
          color: index === highlighted || option.id === modelValue ? UI.blue2 : UI.text,
        }"
        @mousedown.prevent
        @mouseenter="highlighted = index"
        @click="selectOption(option)"
      >
        {{ option.label }}
      </div>

      <div
        v-if="showCreate"
        data-testid="combo-create"
        :style="{
          padding: '7px 10px',
          borderRadius: '7px',
          cursor: 'pointer',
          fontSize: '13px',
          color: UI.blue,
          borderTop: filtered.length > 0 ? `1px solid ${UI.borderSub}` : 'none',
          marginTop: filtered.length > 0 ? '4px' : 0,
          paddingTop: filtered.length > 0 ? '10px' : '7px',
        }"
        @mousedown.prevent
        @click="selectCreate"
      >
        <slot name="create" :query="query">Criar “{{ query }}”</slot>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ui-input {
  transition: border-color 0.12s ease;
}
.ui-input:hover {
  border-color: #94a3b8;
}
.ui-input:focus-within {
  border-color: #2563eb;
}
</style>
